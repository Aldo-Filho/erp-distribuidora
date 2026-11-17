package com.pi.erp.preference;

import com.pi.erp.auth.CurrentUser;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences/current")
public class ThemePreferenceController {

    @Autowired
    private ThemePreferenceRepository repository;

    @GetMapping
    public ThemePreference find() {
        return repository.find(CurrentUser.requireId());
    }

    @org.springframework.transaction.annotation.Transactional
    @PatchMapping
    public ThemePreference update(@RequestBody Map<String, String> data) {
        Long userId = CurrentUser.requireId();
        if (
            data.size() != 1 ||
            !data.containsKey("theme") ||
            (data.get("theme") != null && !Set.of("LIGHT", "DARK").contains(data.get("theme")))
        ) throw new IllegalArgumentException(
            "Informe theme como LIGHT, DARK ou null para acompanhar o sistema."
        );
        repository.update(userId, data.get("theme"));
        return repository.find(userId);
    }
}
