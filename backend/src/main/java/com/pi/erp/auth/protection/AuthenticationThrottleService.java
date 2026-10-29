package com.pi.erp.auth.protection;

import com.pi.erp.auth.config.AuthenticationProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationThrottleService {

    private record Window(long start, int count, long blockedUntil) {}

    private final Map<String, Window> windows = new HashMap<>();
    private Semaphore argon;

    @Autowired
    private AuthenticationProperties properties;

    public synchronized boolean allow(String key, int maximum, long seconds) {
        long now = Instant.now().getEpochSecond();
        windows.entrySet().removeIf(entry -> entry.getValue().start() + 3600 < now);
        Window window = windows.get(key);
        if (window == null || window.start() + seconds <= now) {
            if (windows.size() >= 10000 && window == null) {
                return false;
            }
            windows.put(key, new Window(now, 1, 0));
            return true;
        }
        if (window.count() >= maximum) {
            return false;
        }
        windows.put(key, new Window(window.start(), window.count() + 1, window.blockedUntil()));
        return true;
    }

    public synchronized void checkAccount(String key) {
        Window window = windows.get("failure:" + key);
        if (window != null && window.blockedUntil() > Instant.now().getEpochSecond()) {
            throw limited();
        }
    }

    public synchronized void failed(String key) {
        long now = Instant.now().getEpochSecond();
        String name = "failure:" + key;
        Window previous = windows.get(name);
        int failures = previous == null || previous.start() + 900 <= now ? 1 : previous.count() + 1;
        long start = failures == 1 ? now : previous.start();
        long delay = failures < 5 ? 0 : Math.min(900, 30L << Math.min(failures - 5, 5));
        if (windows.size() < 10000 || previous != null) {
            windows.put(name, new Window(start, failures, now + delay));
        }
    }

    public synchronized void succeeded(String key) {
        windows.remove("failure:" + key);
    }

    public <T> T passwordWork(Supplier<T> work) {
        Semaphore permits;
        synchronized (this) {
            if (argon == null) {
                argon = new Semaphore(Math.max(1, properties.getArgonConcurrency()));
            }
            permits = argon;
        }
        if (!permits.tryAcquire()) {
            throw limited();
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }

    public ResponseStatusException limited() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please try again later.");
    }
}
