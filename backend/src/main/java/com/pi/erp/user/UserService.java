package com.pi.erp.user;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.mail.AccessEmailService;
import com.pi.erp.auth.mail.AccessMailService;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.user.config.role.Role;
import com.pi.erp.user.config.role.RoleRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private AccessMailService accessMailService;

    @Autowired
    private AccessEmailService accessEmailService;

    @Autowired
    private UserRepository repository;

    @Autowired
    private RoleRepository roleRepository;

    public List<User> search(UserFilter filter) {
        Specification<User> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }
        if (filter.email() != null && !filter.email().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + filter.email().toLowerCase() + "%")
            );
        }
        if (filter.roleId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("role").get("id"), filter.roleId())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), filter.active())
            );
        }

        return repository.findAll(spec);
    }

    public User register(RequestUserDTO data) {
        throw new IllegalArgumentException("Create users through invitations.");
    }

    @Transactional
    public User update(Long id, PatchUserDTO data) {
        accessAdministrationService.lock();
        if (data.password() != null) {
            throw new IllegalArgumentException("Use the dedicated password recovery operation.");
        }
        User user = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String name = data.name() == null ? user.getName() : data.name();
        String oldEmail = user.getEmail();
        String email = accessEmailService.normalize(
            data.email() == null ? user.getEmail() : data.email()
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException("User name is required.");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("User email is required.");
        }
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException("User email already exists.");
        }

        Role role = data.roleId() == null ? user.getRole() : findRole(data.roleId());

        user.setName(name);
        user.setEmail(email);
        user.setRole(role);

        if (data.active() != null) {
            user.setActive(data.active());
        }

        if (!oldEmail.equals(email) || !Boolean.TRUE.equals(user.getActive())
            || !Boolean.TRUE.equals(role.getActive())) {
            user.setCredentialVersion(user.getCredentialVersion() + 1);
            accessAdministrationService.invalidate(id);
        }
        User savedUser = repository.saveAndFlush(user);
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("USER_UPDATED", id);
        if (!oldEmail.equals(email)) {
            accessMailService.notice(oldEmail, "Your ERP access email was changed.");
        }
        return savedUser;
    }

    @Transactional
    public void delete(Long id) {
        accessAdministrationService.lock();
        User user = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        accessAdministrationService.invalidate(id);
        repository.delete(user);
        repository.flush();
        accessAdministrationService.verifyAdministrator();
        accessAdministrationService.audit("USER_DELETED", id, user.getName());
    }

    private Role findRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("User role is required.");
        }

        return roleRepository
            .findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found."));
    }
}
