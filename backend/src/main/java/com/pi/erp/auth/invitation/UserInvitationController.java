package com.pi.erp.auth.invitation;

import com.pi.erp.auth.audit.SecurityAuditController;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInvitationController {

    @Autowired
    private UserInvitationService service;

    @GetMapping("/user-invitations")
    public ResponseEntity<Page<UserInvitation>> search(
        @RequestParam(required = false) String email,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(
            service.search(email, SecurityAuditController.pageable(page, size))
        );
    }

    @PostMapping("/user-invitations")
    public ResponseEntity<UserInvitation> register(
        @RequestBody @Valid RequestUserInvitationDTO data
    ) {
        return ResponseEntity.status(201).body(service.register(data));
    }

    @PostMapping("/user-invitations/{id}/deliveries")
    public ResponseEntity<Void> resend(@PathVariable UUID id) {
        service.resend(id);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/user-invitations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/invitation-acceptances")
    public ResponseEntity<Void> accept(@RequestBody @Valid RequestInvitationAcceptanceDTO data) {
        service.accept(data);
        return ResponseEntity.status(201).build();
    }
}
