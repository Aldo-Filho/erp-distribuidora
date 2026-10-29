package com.pi.erp.user.config.role;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.UserRepository;
import com.pi.erp.user.config.RolePermissionRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private RoleRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    public List<Role> search(RoleFilter filter) {
        Specification<Role> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), filter.active())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public Role register(RequestRoleDTO data) {
        accessAdministrationService.lock();
        Role role = new Role(data);
        role.setDescription(
            data.description() == null || data.description().isBlank() ? null : data.description()
        );

        Role savedRole = repository.saveAndFlush(role);
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("ROLE_CREATED", savedRole.getId());
        return savedRole;
    }

    @Transactional
    public Role update(Long id, PatchRoleDTO data) {
        accessAdministrationService.lock();
        Role role = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        if (data.name() != null && data.name().isBlank()) {
            throw new IllegalArgumentException("Role name is required.");
        }

        if (data.name() != null) {
            role.setName(data.name());
        }
        if (data.description() != null) {
            role.setDescription(data.description().isBlank() ? null : data.description());
        }
        if (data.active() != null) {
            role.setActive(data.active());
        }

        if (!Boolean.TRUE.equals(role.getActive())) {
            accessAdministrationService.invalidateRole(id);
        }
        Role savedRole = repository.saveAndFlush(role);
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("ROLE_UPDATED", id);
        return savedRole;
    }

    @Transactional
    public void delete(Long id) {
        accessAdministrationService.lock();
        Role role = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found."));

        if (userRepository.existsByRoleId(id)) {
            throw new IllegalArgumentException("Role is linked to users and cannot be deleted.");
        }
        if (rolePermissionRepository.existsByRoleId(id)) {
            throw new IllegalArgumentException("Role is linked to permissions and cannot be deleted.");
        }

        repository.delete(role);
        repository.flush();
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("ROLE_DELETED", id, role.getName());
    }
}
