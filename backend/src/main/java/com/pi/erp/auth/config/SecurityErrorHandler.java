package com.pi.erp.auth.config;

import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.exception.ResponseErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityAuditService securityAuditService;

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(
            response.getOutputStream(),
            new ResponseErrorDTO(
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                LocalDateTime.now()
            )
        );
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        write(response, 401, "Authentication is required.");
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        securityAuditService.failure("ACCESS_DENIED");
        write(response, 403, "Permission or CSRF validation failed.");
    }
}
