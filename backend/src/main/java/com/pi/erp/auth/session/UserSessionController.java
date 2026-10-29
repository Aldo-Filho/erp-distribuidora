package com.pi.erp.auth.session;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditController;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserSessionController {

    @Autowired
    private UserSessionService service;

    @GetMapping("/auth/sessions")
    public ResponseEntity<List<UserSession>> search() {
        return ResponseEntity.ok(service.sessions(CurrentUser.requireId()));
    }

    @DeleteMapping("/auth/sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.revoke(CurrentUser.requireId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{id}/sessions")
    public ResponseEntity<Page<UserSession>> search(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        CurrentUser.require("security.sessions.manage");
        var pageable = SecurityAuditController.pageable(page, size);
        List<UserSession> sessions = service.sessions(id);
        int start = (int) Math.min(pageable.getOffset(), sessions.size());
        int end = Math.min(start + size, sessions.size());
        return ResponseEntity.ok(
            new PageImpl<>(sessions.subList(start, end), pageable, sessions.size())
        );
    }

    @DeleteMapping("/user/{userId}/sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable UUID id) {
        CurrentUser.require("security.sessions.manage");
        service.revoke(userId, id);
        return ResponseEntity.noContent().build();
    }
}
