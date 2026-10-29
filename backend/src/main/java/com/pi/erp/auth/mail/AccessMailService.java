package com.pi.erp.auth.mail;

import com.pi.erp.auth.config.AuthenticationProperties;
import com.pi.erp.auth.password.AccessToken;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class AccessMailService {

    public record Delivery(String email, String text, UUID invitationId, String tokenHash) {}

    @Autowired
    private AuthenticationProperties properties;

    @Autowired
    private ApplicationEventPublisher publisher;

    public void link(String email, String route, String token, String validity, UUID invitationId) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            publisher.publishEvent(
                new Delivery(email, null, invitationId, AccessToken.hash(token))
            );
            return;
        }
        URI uri;
        try {
            uri = URI.create(base);
        } catch (IllegalArgumentException exception) {
            publisher.publishEvent(
                new Delivery(email, null, invitationId, AccessToken.hash(token))
            );
            return;
        }
        if (
            uri.getHost() == null ||
            uri.getUserInfo() != null ||
            uri.getQuery() != null ||
            uri.getFragment() != null ||
            (!"https".equals(uri.getScheme()) &&
                !(
                    "http".equals(uri.getScheme()) &&
                    ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))
                ))
        ) {
            publisher.publishEvent(
                new Delivery(email, null, invitationId, AccessToken.hash(token))
            );
            return;
        }
        String url = base.replaceAll("/+$", "") + route + "#token=" + token;
        publisher.publishEvent(
            new Delivery(
                email,
                "ERP Distribuidora\n\nAccess link (valid for " +
                    validity +
                    "):\n" +
                    url +
                    "\n\nIf you did not request this message, ignore it.",
                invitationId,
                AccessToken.hash(token)
            )
        );
    }

    public void notice(String email, String text) {
        publisher.publishEvent(new Delivery(email, text, null, null));
    }
}
