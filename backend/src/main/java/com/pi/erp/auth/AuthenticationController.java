package com.pi.erp.auth;

import com.pi.erp.auth.session.LoginChallengeView;
import com.pi.erp.auth.session.RequestSessionReplacementDTO;
import com.pi.erp.auth.session.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService service;

    @Autowired
    private UserSessionService userSessionService;

    public record CsrfView(String token, String headerName) {}

    @GetMapping("/csrf")
    public ResponseEntity<CsrfView> csrf(CsrfToken token) {
        return ResponseEntity.ok(new CsrfView(token.getToken(), token.getHeaderName()));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(
        @RequestBody @Valid RequestLoginDTO data,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Object view = service.login(data, request, response);
        return ResponseEntity.status(view instanceof LoginChallengeView ? 409 : 200).body(view);
    }

    @PostMapping("/sessionReplacement")
    public ResponseEntity<Object> replace(
        @RequestBody @Valid RequestSessionReplacementDTO data,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Object view = service.replace(data, request, response);
        return ResponseEntity.status(view instanceof LoginChallengeView ? 409 : 200).body(view);
    }

    @GetMapping("/session")
    public ResponseEntity<SessionView> session(HttpServletRequest request) {
        return ResponseEntity.ok(userSessionService.view(request));
    }

    @PostMapping("/activity")
    public ResponseEntity<Void> activity(HttpServletRequest request) {
        userSessionService.activity(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        userSessionService.logout(request);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addHeader(
            "Set-Cookie",
            "ERP_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" +
                (request.isSecure() ? "; Secure" : "")
        );
        return ResponseEntity.noContent().build();
    }
}
