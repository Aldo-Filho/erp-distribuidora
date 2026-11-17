package com.pi.erp.company;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.exception.ResourceNotFoundException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppearanceService {

    public static final Set<String> PALETTES = Set.of(
        "jade",
        "ocean",
        "violet",
        "terracotta",
        "amber",
        "graphite"
    );

    @Autowired
    private AppearanceRepository repository;

    @Autowired
    private LogoStorage storage;

    @Autowired
    private SecurityAuditService audit;

    @Transactional(readOnly = true)
    public Appearance find() {
        CurrentUser.requireId();
        return repository.find(false).response();
    }

    @Transactional
    public Appearance palette(String palette, long version) {
        CurrentUser.require("company.appearance.manage");
        if (palette == null || !PALETTES.contains(palette)) throw new IllegalArgumentException(
            "Selecione uma paleta disponível."
        );
        var current = current(version);
        var saved = repository.update(palette, current.filename(), CurrentUser.requireId());
        audit.record("COMPANY_APPEARANCE_UPDATED", "1", "SUCCESS");
        return saved;
    }

    @Transactional
    public Appearance logo(MultipartFile file, long version) {
        CurrentUser.require("company.appearance.manage");
        var current = current(version);
        String filename = storage.save(file);
        cleanup(filename, current.filename());
        var saved = repository.update(current.palette(), filename, CurrentUser.requireId());
        audit.record("COMPANY_LOGO_UPDATED", "1", "SUCCESS");
        return saved;
    }

    @Transactional
    public Appearance removeLogo(long version) {
        CurrentUser.require("company.appearance.manage");
        var current = current(version);
        cleanup(null, current.filename());
        var saved = repository.update(current.palette(), null, CurrentUser.requireId());
        audit.record("COMPANY_LOGO_REMOVED", "1", "SUCCESS");
        return saved;
    }

    public record Logo(byte[] bytes, String contentType) {}

    // Serialize the file read against replacement/deletion of the same singleton.
    @Transactional
    public Logo logo() {
        CurrentUser.requireId();
        var current = repository.find(true);
        if (current.filename() == null) throw new ResourceNotFoundException("Logo não cadastrado.");
        return new Logo(
            storage.read(current.filename()),
            current.filename().endsWith(".png") ? "image/png" : "image/jpeg"
        );
    }

    private AppearanceRepository.Settings current(long version) {
        var current = repository.find(true);
        if (version < 0 || current.version() != version) throw new ResponseStatusException(
            HttpStatus.CONFLICT
        );
        return current;
    }

    private void cleanup(String created, String replaced) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) storage.discard(replaced);
                    else storage.discard(created);
                }
            }
        );
    }
}
