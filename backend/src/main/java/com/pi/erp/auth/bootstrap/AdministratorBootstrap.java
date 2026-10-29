package com.pi.erp.auth.bootstrap;

import com.pi.erp.auth.AccessAdministrationService;
import com.pi.erp.auth.AccessQueryService;
import com.pi.erp.auth.config.AuthenticationProperties;
import com.pi.erp.auth.mail.AccessEmailService;
import com.pi.erp.auth.password.AccessToken;
import com.pi.erp.user.RequestUserDTO;
import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import com.pi.erp.user.config.role.RoleRepository;
import java.io.Console;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "erp.auth", name = "bootstrap-enabled", havingValue = "true")
public class AdministratorBootstrap implements ApplicationRunner {

    @Autowired
    private AccessAdministrationService accessAdministrationService;

    @Autowired
    private AccessQueryService accessQueryService;

    @Autowired
    private AuthenticationProperties properties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessEmailService accessEmailService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments arguments) {
        if (accessAdministrationService.hasAdministrator()) {
            throw new IllegalStateException(
                "An administrator already exists. Disable bootstrap mode before starting."
            );
        }
        Console console = System.console();
        if (console == null || properties.getBootstrapRoleId() == null) {
            throw new IllegalStateException(
                "Bootstrap requires a protected interactive console and an explicit role ID."
            );
        }
        String name = console.readLine("Administrator name: ");
        String email = accessEmailService.normalize(console.readLine("Administrator email: "));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A valid administrator name is required.");
        }
        char[] password = console.readPassword("Password (15-128 characters, input hidden): ");
        char[] confirmation = console.readPassword("Confirm password (input hidden): ");
        String hash;
        try {
            if (
                password == null || confirmation == null || !Arrays.equals(password, confirmation)
            ) {
                throw new IllegalArgumentException("Password confirmation does not match.");
            }
            String value = new String(password);
            AccessToken.password(value);
            hash = passwordEncoder.encode(value);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
        transactionTemplate.executeWithoutResult(status -> {
            accessAdministrationService.lock();
            if (accessAdministrationService.hasAdministrator()) {
                throw new IllegalArgumentException("An administrator already exists.");
            }
            Long roleId = properties.getBootstrapRoleId();
            var role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Bootstrap role does not exist."));
            if (
                !Boolean.TRUE.equals(role.getActive()) ||
                !Set.copyOf(accessQueryService.permissions(roleId)).containsAll(
                    Set.of("access.users.manage", "access.roles.manage", "security.settings.manage")
                )
            ) {
                throw new IllegalArgumentException(
                    "Bootstrap role lacks the required administrative permissions."
                );
            }
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException(
                    "User email already exists. Existing users will not be promoted."
                );
            }
            User user = userRepository.saveAndFlush(
                new User(new RequestUserDTO(name.trim(), email, null, roleId, true), role, hash)
            );
            accessAdministrationService.verifyAdministrator();
            accessAdministrationService.audit("ADMINISTRATOR_BOOTSTRAPPED", user.getId());
        });
        console.printf("Administrator created. Disable bootstrap mode and restart normally.%n");
        // Stop after the one-time setup instead of exposing an application with bootstrap enabled.
        throw new IllegalStateException("Bootstrap finished. Disable bootstrap mode and restart.");
    }
}
