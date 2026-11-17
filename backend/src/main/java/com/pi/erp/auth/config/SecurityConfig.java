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
        "/auth/sessions",
        Set.of("POST"),
        "/auth/session-replacements",
        Set.of("POST"),
        "/auth/sessions/current",
        Set.of("DELETE"),
        "/auth/password-recoveries",
        Set.of("POST"),
        "/auth/password-resets",
        Set.of("POST"),
        "/auth/invitation-acceptances",
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
        if (path.equals("/dashboard") || path.matches("/dashboard/widgets/[^/]+/details")) return read;
        if (path.equals("/preferences/current/dashboard")) return read || method.equals("PUT") || method.equals("DELETE");
        if (Set.of("financial-categories", "bank-accounts").contains(root)) {
            if (read) return keys.contains("finance.read") || keys.contains("finance.configure");
            return (method.equals("POST") || method.equals("PATCH")) && keys.contains("finance.configure");
        }
        if (root.equals("cash-flow")) return read && keys.contains("finance.read");
        if (Set.of("receivables", "payables").contains(root)) {
            if (read) return keys.contains("finance.read");
            if (method.equals("POST") && path.matches("/(receivables|payables)/[0-9]+/settlements"))
                return keys.contains("finance.settle");
            if (method.equals("POST") && path.matches("/(receivables|payables)/[0-9]+/settlements/[0-9]+/reversals"))
                return keys.contains("finance.reverse");
            return false;
        }
        if (root.equals("product-barcodes")) {
            if (read && path.equals("/product-barcodes/lookup")) return keys.contains("products.read") || keys.contains("stock.read");
            if (read) return keys.contains("products.read");
            return (method.equals("POST") || method.equals("PATCH")) && keys.contains("products.update");
        }
        if (root.equals("purchase-suggestions")) {
            if (read) return keys.contains("purchases.read");
            return method.equals("POST") && path.equals("/purchase-suggestions") && keys.contains("purchases.suggest") && keys.contains("purchases.create") && keys.contains("purchases.read");
        }
        if (root.equals("auth")) {
            return (
                (read && Set.of("/auth/sessions/current", "/auth/sessions").contains(path)) ||
                (method.equals("DELETE") && path.matches("/auth/sessions/[^/]+")) ||
                (method.equals("POST") && path.equals("/auth/session-activities")) ||
                (method.equals("PATCH") && path.equals("/auth/password"))
            );
        }
        if (path.equals("/company/profile")) return keys.contains("company.read") &&
            (read || (method.equals("PUT") && keys.contains("company.manage")));
        if (path.equals("/company/appearance")) return (
            read || (method.equals("PATCH") && keys.contains("company.appearance.manage"))
        );
        if (path.equals("/company/appearance/logo")) return (
            read ||
            (Set.of("PUT", "DELETE").contains(method) && keys.contains("company.appearance.manage"))
        );
        if (path.equals("/preferences/current")) return read || method.equals("PATCH");
        if (root.equals("permissions")) {
            // Keep the existing read-only registry's 405 response for authenticated writes.
            return !read || keys.contains("access.roles.manage");
        }
        if (root.equals("user-invitations")) return keys.contains("access.users.manage");
        if (root.equals("users")) {
            if (path.equals("/users") && method.equals("POST")) return true;
            if (path.matches("/users/[^/]+/sessions(/[^/]+)?")) {
                return keys.contains("security.sessions.manage");
            }
            return (
                keys.contains("access.users.manage") ||
                (read && keys.contains("security.sessions.manage"))
            );
        }
        if (root.equals("roles")) {
            return (
                keys.contains("access.roles.manage") ||
                (read && !path.contains("/permissions") && keys.contains("access.users.manage"))
            );
        }
        if (path.equals("/security/settings")) return keys.contains("security.settings.manage");
        if (path.equals("/security/audit-events") && read) return keys.contains(
            "security.audit.read"
        );
        String module = switch (root) {
            case "customers", "customer-addresses" -> "customers";
            case
                "suppliers",
                "supplier-addresses",
                "product-suppliers",
                "product-supplier-options" -> "suppliers";
            case
                "products",
                "product-catalogs",
                "product-presentations",
                "brands",
                "categories" -> "products";
            case "warehouses", "warehouse-addresses" -> "warehouses";
            case "stock-items", "stock-movements" -> "stock";
            case "price-tables", "price-table-items" -> "prices";
            case "orders", "order-items" -> "orders";
            case "purchases", "purchase-items" -> "purchases";
            default -> null;
        };
        if (module == null) return false;
        String action;
        if (read) action = "read";
        else if (root.equals("stock-items")) action = "configure";
        else if (root.equals("stock-movements") && method.equals("POST")) action = "adjust";
        else if (
            root.equals("product-suppliers") ||
            root.equals("product-supplier-options") ||
            path.matches("/suppliers/[^/]+/(brands|categories)(/[^/]+)?")
        ) action = "update";
        else if (method.equals("POST") && path.matches("/orders/[^/]+/confirmations")) action =
            "confirm";
        else if (method.equals("POST") && path.matches("/orders/[^/]+/cancellations")) action =
            "cancel";
        else if (method.equals("POST") && path.matches("/orders/[^/]+/dispatches")) action =
            "dispatch";
        else if (method.equals("POST") && path.matches("/purchases/[^/]+/submissions")) action =
            "send";
        else if (method.equals("POST") && path.matches("/purchases/[^/]+/confirmations")) action =
            "confirm";
        else if (method.equals("POST") && path.matches("/purchases/[^/]+/cancellations")) action =
            "cancel";
        else if (method.equals("POST") && path.matches("/purchases/[^/]+/receipts")) action =
            "receive";
        else action = switch (method) {
            case "POST" -> "create";
            case "PATCH" -> "update";
            case "DELETE" -> "delete";
            default -> "unsupported";
        };
        return keys.contains(module + "." + action);
    }
}
