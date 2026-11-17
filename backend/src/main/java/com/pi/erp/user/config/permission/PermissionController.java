package com.pi.erp.user.config.permission;

import com.pi.erp.api.ApiPagination;
import com.pi.erp.auth.PermissionKey;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.exception.ResponseErrorDTO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    @Autowired
    private PermissionRepository repository;

    @Autowired
    private PermissionService service;

    @GetMapping
    public ResponseEntity<Page<Permission>> search(
        PermissionFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(service.search(filter, ApiPagination.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .filter(permission -> PermissionKey.ALL.contains(permission.getPermissionKey()))
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }

    @PostMapping
    public ResponseEntity<ResponseErrorDTO> register() {
        return readOnly();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ResponseErrorDTO> update(@PathVariable Long id) {
        return readOnly();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseErrorDTO> delete(@PathVariable Long id) {
        return readOnly();
    }

    private ResponseEntity<ResponseErrorDTO> readOnly() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            new ResponseErrorDTO(
                405,
                "Method Not Allowed",
                "Permission catalog is read-only.",
                LocalDateTime.now()
            )
        );
    }
}
