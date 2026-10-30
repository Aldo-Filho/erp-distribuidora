package com.pi.erp.auth.config;

import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.ErpPrincipal;
import com.pi.erp.auth.protection.AuthenticationThrottleFilter;
import com.pi.erp.auth.protection.AuthenticationThrottleService;
import com.pi.erp.auth.session.SessionPolicyFilter;
import com.pi.erp.auth.session.UserSessionRepository;
import com.pi.erp.auth.session.UserSessionService;
import com.pi.erp.auth.settings.SecuritySettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class SecurityConfig {

    private static final Map<String, Set<String>> PUBLIC = Map.of(
        "/auth/csrf",
        Set.of("GET"),
        "/auth/login",
        Set.of("POST"),
        "/auth/sessionReplacement",
        Set.of("POST"),
        "/auth/logout",
        Set.of("POST"),
        "/auth/passwordRecovery",
        Set.of("POST"),
        "/auth/passwordReset",
        Set.of("POST"),
        "/auth/invitationAcceptance",
        Set.of("POST")
    );

    @Autowired
    private SecurityErrorHandler securityErrorHandler;

    @Bean
    public AuthenticationManager authenticationManager() {
        // AuthenticationService handles ERP credentials explicitly; suppress Boot's fallback account.
        return authentication -> {
            throw new BadCredentialsException("Use the ERP authentication endpoints.");
        };
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public ThreadPoolTaskExecutor accessMailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("access-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }

    public static boolean publicRoute(HttpServletRequest request) {
        return PUBLIC.getOrDefault(request.getServletPath(), Set.of()).contains(
            request.getMethod()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        UserSessionRepository userSessionRepository,
        UserSessionService userSessionService,
        AccessQueryService accessQueryService,
        SecuritySettingsRepository securitySettingsRepository,
        AuthenticationThrottleService authenticationThrottleService,
        AuthenticationProperties properties
    ) throws Exception {
        http.formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .requestCache(cache -> cache.disable())
            .securityContext(context -> context.requireExplicitSave(true))
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(csrfTokenRepository())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .exceptionHandling(errors ->
                errors
                    .authenticationEntryPoint(securityErrorHandler)
                    .accessDeniedHandler(securityErrorHandler)
            )
            .headers(headers ->
                headers.referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                )
            )
            .authorizeHttpRequests(routes ->
                routes
                    .requestMatchers(SecurityConfig::publicRoute)
                    .permitAll()
                    .anyRequest()
                    .access((authentication, context) ->
                        new AuthorizationDecision(
                            authorized(context.getRequest(), authentication.get())
                        )
                    )
            );
        http.addFilterBefore(
            new SessionPolicyFilter(
                userSessionRepository,
                userSessionService,
                accessQueryService,
                securitySettingsRepository,
                securityErrorHandler
            ),
            CsrfFilter.class
        );
        http.addFilterBefore(
            new AuthenticationThrottleFilter(
                authenticationThrottleService,
                properties,
                securityErrorHandler
            ),
            SessionPolicyFilter.class
        );
        return http.build();
    }

    private boolean authorized(HttpServletRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ErpPrincipal)) {
            return false;
        }
        String path = request.getServletPath();
        String method = request.getMethod();
        String[] parts = path.split("/");
        String root = parts.length > 1 ? parts[1] : "";
        Set<String> keys = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        boolean read = method.equals("GET") || method.equals("HEAD");
        if (root.equals("auth")) {
            return (
                (read && Set.of("/auth/session", "/auth/sessions").contains(path)) ||
                (method.equals("DELETE") && path.matches("/auth/sessions/[^/]+")) ||
                (method.equals("POST") && path.equals("/auth/activity")) ||
                (method.equals("PATCH") && path.equals("/auth/password"))
            );
        }
        if (root.equals("permission")) {
            // Writes reach read-only controller to return the approved 405 contract.
            return !read || keys.contains("access.roles.manage");
        }
        if (root.equals("user")) {
            if (path.equals("/user") && method.equals("POST")) {
                return true;
            }
            if (path.matches("/user/[^/]+/sessions(/[^/]+)?")) {
                return keys.contains("security.sessions.manage");
            }
            return (
                keys.contains("access.users.manage") ||
                (read &&
                    !path.startsWith("/user/invitation") &&
                    keys.contains("security.sessions.manage"))
            );
        }
        if (root.equals("role")) {
            return (
                keys.contains("access.roles.manage") ||
                (read && !path.contains("/permissions") && keys.contains("access.users.manage"))
            );
        }
        if (path.equals("/security/settings")) {
            return keys.contains("security.settings.manage");
        }
        if (path.equals("/security/audit/search") && read) {
            return keys.contains("security.audit.read");
        }
        String module = switch (root) {
            case "customer", "customerAddress" -> "customers";
            case
                "supplier",
                "supplierAddress",
                "productSupplier",
                "productSupplierOption" -> "suppliers";
            case
                "product",
                "productCatalog",
                "productPresentation",
                "brand",
                "category" -> "products";
            case "warehouse", "warehouseAddress" -> "warehouses";
            case "stockItem", "stockMovement" -> "stock";
            case "price-table", "priceTableItem" -> "prices";
            case "order", "orderItem" -> "orders";
            case "purchase", "purchaseItem" -> "purchases";
            default -> null;
        };
        if (module == null) {
            return false;
        }
        String action;
        if (read) {
            action = "read";
        } else if (root.equals("stockItem")) {
            action = "configure";
        } else if (root.equals("stockMovement") && method.equals("POST")) {
            action = "adjust";
        } else if (
            root.equals("productSupplier") ||
            root.equals("productSupplierOption") ||
            path.matches("/supplier/[^/]+/(brands|categories)(/[^/]+)?")
        ) {
            action = "update";
        } else if (path.matches("/order/[^/]+/dispatch") && method.equals("POST")) {
            action = "dispatch";
        } else if (path.matches("/purchase/[^/]+/receive") && method.equals("POST")) {
            action = "receive";
        } else {
            action = switch (method) {
                case "POST" -> "create";
                case "PATCH" -> "update";
                case "DELETE" -> "delete";
                default -> "unsupported";
            };
        }
        return keys.contains(module + "." + action);
    }
}
