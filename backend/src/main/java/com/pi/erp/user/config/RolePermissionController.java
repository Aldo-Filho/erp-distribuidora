package com.pi.erp.user.config;

import com.pi.erp.api.ApiPagination;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roles/{roleId}/permissions")
public class RolePermissionController {

    @Autowired
    private RolePermissionService service;

    @GetMapping
    public ResponseEntity<Page<RolePermission>> search(
        @PathVariable Long roleId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(
            service.search(roleId, ApiPagination.pageable(page, size, Sort.by("permission.id")))
        );
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
    public ResponseEntity<Void> delete(@PathVariable Long roleId, @PathVariable Long permissionId) {
        service.delete(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
}
