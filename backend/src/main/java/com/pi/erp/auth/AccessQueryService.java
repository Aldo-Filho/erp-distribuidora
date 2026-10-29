package com.pi.erp.auth;

import com.pi.erp.user.User;
import com.pi.erp.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessQueryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    public User findActive(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (
            user == null ||
            !Boolean.TRUE.equals(user.getActive()) ||
            !Boolean.TRUE.equals(user.getRole().getActive())
        ) {
            return null;
        }
        return user;
    }

    public List<String> permissions(Long roleId) {
        return jdbc
            .queryForList(
                """
                SELECT p.permission_key FROM role_permissions rp
                JOIN permissions p USING(permission_id) WHERE rp.role_id = ? ORDER BY p.permission_key
                """,
                String.class,
                roleId
            )
            .stream()
            .filter(PermissionKey.ALL::contains)
            .toList();
    }
}
