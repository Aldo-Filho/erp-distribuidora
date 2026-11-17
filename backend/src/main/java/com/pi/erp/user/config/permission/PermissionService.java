package com.pi.erp.user.config.permission;

import com.pi.erp.auth.PermissionKey;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.config.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository repository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    public Page<Permission> search(PermissionFilter filter, Pageable pageable) {
        Specification<Permission> spec = Specification.allOf();

        if (filter.module() != null && !filter.module().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("module"), filter.module()));
        }
        if (filter.action() != null && !filter.action().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), filter.action()));
        }
        if (filter.permissionKey() != null && !filter.permissionKey().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("permissionKey"), filter.permissionKey())
            );
        }

        spec = spec.and((root, query, cb) -> root.get("permissionKey").in(PermissionKey.ALL));
        return repository.findAll(spec, pageable);
    }

    public Permission register(RequestPermissionDTO data) {
        if (repository.existsByPermissionKey(data.permissionKey())) {
            throw new IllegalArgumentException("Permission key already exists.");
        }

        Permission permission = new Permission(data);
        return repository.save(permission);
    }

    public Permission update(Long id, PatchPermissionDTO data) {
        Permission permission = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found."));

        String module = data.module() == null ? permission.getModule() : data.module();
        String action = data.action() == null ? permission.getAction() : data.action();
        String permissionKey =
            data.permissionKey() == null ? permission.getPermissionKey() : data.permissionKey();

        if (module.isBlank()) {
            throw new IllegalArgumentException("Permission module is required.");
        }
        if (action.isBlank()) {
            throw new IllegalArgumentException("Permission action is required.");
        }
        if (permissionKey.isBlank()) {
            throw new IllegalArgumentException("Permission key is required.");
        }
        if (repository.existsByPermissionKeyAndIdNot(permissionKey, id)) {
            throw new IllegalArgumentException("Permission key already exists.");
        }

        permission.setModule(module);
        permission.setAction(action);
        permission.setPermissionKey(permissionKey);

        return repository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found."));

        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new IllegalArgumentException(
                "Permission is linked to roles and cannot be deleted."
            );
        }

        repository.delete(permission);
    }
}
