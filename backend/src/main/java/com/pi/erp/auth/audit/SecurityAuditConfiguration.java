package com.pi.erp.auth.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityAuditConfiguration implements WebMvcConfigurer {

    @Autowired
    private SecurityAuditService securityAuditService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(
                new HandlerInterceptor() {
                    @Override
                    public void afterCompletion(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Object handler,
                        Exception exception
                    ) {
                        if (response.getStatus() >= 400 && !request.getMethod().equals("GET")) {
                            securityAuditService.failure("ACCESS_OPERATION_REJECTED");
                        }
                    }
                }
            )
            .addPathPatterns("/auth/**", "/user/**", "/role/**", "/permission/**", "/security/**");
    }
}
