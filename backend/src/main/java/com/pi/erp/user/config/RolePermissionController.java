package com.pi.erp.user.config;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role/{roleId}/permissions")
public class RolePermissionController {

    @Autowired
    private RolePermissionService service;

    @GetMapping
    public ResponseEntity<List<RolePermission>> search(@PathVariable Long roleId) {
        return ResponseEntity.ok(service.search(roleId));
    }

    @PostMapping
    public ResponseEntity<RolePermission> register(
        @PathVariable Long roleId,
        @RequestBody @Valid RequestRolePermissionDTO data
    ) {
        RolePermission rolePermission = service.register(roleId, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(rolePermission);
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long roleId,
        @PathVariable Long permissionId
    ) {
        service.delete(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
}
