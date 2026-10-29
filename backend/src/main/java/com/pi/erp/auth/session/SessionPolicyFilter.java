package com.pi.erp.auth.session;

import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.ErpPrincipal;
import com.pi.erp.auth.config.SecurityConfig;
import com.pi.erp.auth.config.SecurityErrorHandler;
import com.pi.erp.auth.settings.SecuritySettingsRepository;
import com.pi.erp.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionPolicyFilter extends OncePerRequestFilter {

    private final UserSessionRepository repository;
    private final UserSessionService service;
    private final AccessQueryService accessQueryService;
    private final SecuritySettingsRepository securitySettingsRepository;
    private final SecurityErrorHandler securityErrorHandler;

    public SessionPolicyFilter(
        UserSessionRepository repository,
        UserSessionService service,
        AccessQueryService accessQueryService,
        SecuritySettingsRepository securitySettingsRepository,
        SecurityErrorHandler securityErrorHandler
    ) {
        this.repository = repository;
        this.service = service;
        this.accessQueryService = accessQueryService;
        this.securitySettingsRepository = securitySettingsRepository;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (
            authentication != null &&
            authentication.getPrincipal() instanceof ErpPrincipal principal
        ) {
            User user = accessQueryService.findActive(principal.userId());
            UserSession session =
                request.getSession(false) == null
                    ? null
                    : repository.findBySessionId(request.getSession(false).getId());
            if (
                !service.valid(session, user, securitySettingsRepository.findOne()) ||
                principal.credentialVersion() != user.getCredentialVersion()
            ) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            } else {
                var authorities = accessQueryService
                    .permissions(user.getRole().getId())
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
                );
                SecurityContextHolder.setContext(context);
            }
        }
        if (!SecurityConfig.publicRoute(request) && CurrentUser.idOrNull() == null) {
            // Private unauthenticated writes produce 401 before CSRF is evaluated.
            securityErrorHandler.write(response, 401, "Authentication is required.");
            return;
        }
        chain.doFilter(request, response);
    }
}
