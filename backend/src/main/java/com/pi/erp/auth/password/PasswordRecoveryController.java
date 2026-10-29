package com.pi.erp.auth.password;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordRecoveryController {

    @Autowired
    private PasswordRecoveryService service;

    @PostMapping("/auth/passwordRecovery")
    public ResponseEntity<Void> recovery(@RequestBody @Valid RequestPasswordRecoveryDTO data) {
        service.recovery(data.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/user/{id}/passwordRecovery")
    public ResponseEntity<Void> administrative(@PathVariable Long id) {
        service.administrative(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/passwordReset")
    public ResponseEntity<Void> reset(@RequestBody @Valid RequestPasswordResetDTO data) {
        service.reset(data);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/auth/password")
    public ResponseEntity<Void> update(@RequestBody @Valid PatchPasswordDTO data) {
        service.update(data);
        return ResponseEntity.noContent().build();
    }
}
