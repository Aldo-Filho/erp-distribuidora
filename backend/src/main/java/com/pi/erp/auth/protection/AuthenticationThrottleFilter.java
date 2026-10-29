package com.pi.erp.auth.protection;

import com.pi.erp.auth.config.AuthenticationProperties;
import com.pi.erp.auth.config.SecurityConfig;
import com.pi.erp.auth.config.SecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationThrottleFilter extends OncePerRequestFilter {

    private final AuthenticationThrottleService service;
    private final AuthenticationProperties properties;
    private final SecurityErrorHandler securityErrorHandler;

    public AuthenticationThrottleFilter(
        AuthenticationThrottleService service,
        AuthenticationProperties properties,
        SecurityErrorHandler securityErrorHandler
    ) {
        this.service = service;
        this.properties = properties;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        if (properties.isBootstrapEnabled()) {
            securityErrorHandler.write(
                response,
                503,
                "Access is unavailable during administrator bootstrap."
            );
            return;
        }
        if (!SecurityConfig.publicRoute(request) && !path.equals("/auth/password")) {
            chain.doFilter(request, response);
            return;
        }
        // Only the socket peer is trusted. Forwarded headers must not choose a limiter key.
        boolean recovery = path.equals("/auth/passwordRecovery");
        int maximum = recovery
            ? properties.getRecoveryPerIpPerHour()
            : properties.getLoginPerIpPerMinute();
        if (
            !service.allow(
                (recovery ? "recovery-ip:" : "auth-ip:") + request.getRemoteAddr(),
                maximum,
                recovery ? 3600 : 60
            )
        ) {
            response.setHeader("Retry-After", recovery ? "3600" : "60");
            securityErrorHandler.write(response, 429, "Please try again later.");
            return;
        }
        if (request.getMethod().equals("POST") || request.getMethod().equals("PATCH")) {
            byte[] bytes = request.getInputStream().readNBytes(properties.getMaxBodyBytes() + 1);
            if (bytes.length > properties.getMaxBodyBytes()) {
                securityErrorHandler.write(response, 413, "Request body is too large.");
                return;
            }
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() {
                    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                    return new ServletInputStream() {
                        @Override
                        public int read() {
                            return input.read();
                        }

                        @Override
                        public boolean isFinished() {
                            return input.available() == 0;
                        }

                        @Override
                        public boolean isReady() {
                            return true;
                        }

                        @Override
                        public void setReadListener(ReadListener listener) {
                            throw new UnsupportedOperationException(
                                "Asynchronous reads are not supported."
                            );
                        }
                    };
                }
            };
            chain.doFilter(wrapped, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
