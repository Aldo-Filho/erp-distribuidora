package com.pi.erp.auth.settings;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/security/settings")
public class SecuritySettingsController {

    @Autowired
    private SecuritySettingsRepository repository;

    @Autowired
    private SecuritySettingsService service;

    @GetMapping
    public ResponseEntity<SecuritySettings> findOne() {
        return ResponseEntity.ok(repository.findOne());
    }

    @PatchMapping
    public ResponseEntity<SecuritySettings> update(
        @RequestBody PatchSecuritySettingsDTO data,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(service.update(data, request));
    }
}
