package com.pi.erp.user.config;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.PermissionKey;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.config.permission.Permission;
import com.pi.erp.user.config.permission.PermissionRepository;
import com.pi.erp.user.config.role.Role;
import com.pi.erp.user.config.role.RoleRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionService {

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private RolePermissionRepository repository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    public List<RolePermission> search(Long roleId) {
        findRole(roleId);
        return repository.findAllByRoleId(roleId);
    }

    @Transactional
    public RolePermission register(Long roleId, RequestRolePermissionDTO data) {
        accessAdministrationService.lock();
        Role role = findRole(roleId);
        Permission permission = findPermission(data.permissionId());
        if (!PermissionKey.ALL.contains(permission.getPermissionKey())) {
            throw new IllegalArgumentException("Permission is not part of the executable catalog.");
        }
        RolePermissionId id = new RolePermissionId(roleId, data.permissionId());

        if (repository.existsById(id)) {
            throw new IllegalArgumentException("This permission is already linked to this role.");
        }

        RolePermission rolePermission = new RolePermission(role, permission);
        RolePermission savedRolePermission = repository.saveAndFlush(rolePermission);
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("ROLE_PERMISSION_GRANTED", roleId);
        return savedRolePermission;
    }

    @Transactional
    public void delete(Long roleId, Long permissionId) {
        accessAdministrationService.lock();
        RolePermission rolePermission = repository
            .findById(new RolePermissionId(roleId, permissionId))
            .orElseThrow(() -> new ResourceNotFoundException("Role permission not found."));

        repository.delete(rolePermission);
        repository.flush();
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("ROLE_PERMISSION_REVOKED", roleId);
    }

    private Role findRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required.");
        }

        return roleRepository
            .findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
    }

    private Permission findPermission(Long permissionId) {
        if (permissionId == null) {
            throw new IllegalArgumentException("Permission ID is required.");
        }

        return permissionRepository
            .findById(permissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found."));
    }
}
