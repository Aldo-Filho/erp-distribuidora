package com.pi.erp.company;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.auth.audit.SecurityAuditService;
import com.pi.erp.customer.CustomerTaxId;
import com.pi.erp.customer.PersonType;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyProfileService {
    private static final Set<String> STATES = Set.of(
        "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
        "PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"
    );
    @Autowired private CompanyProfileRepository repository;
    @Autowired private SecurityAuditService audit;

    @Transactional(readOnly = true)
    public CompanyProfile find() {
        CurrentUser.require("company.read");
        return repository.find();
    }

    @Transactional
    public CompanyProfile save(CompanyProfileController.SaveProfile request) {
        CurrentUser.require("company.read");
        CurrentUser.require("company.manage");
        var data = normalize(request);
        var current = repository.link(true);
        if (current.version() != request.version())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (repository.taxIdExists(data.taxId(), current.companyId()))
            throw new IllegalArgumentException("Este CNPJ já pertence a outra empresa cadastrada.");
        repository.save(current.companyId(), data, CurrentUser.requireId());
        var saved = repository.find();
        audit.record("COMPANY_PROFILE_UPDATED", saved.id().toString(), "SUCCESS");
        return saved;
    }

    private CompanyProfile normalize(CompanyProfileController.SaveProfile data) {
        var address = data.address();
        String state = required(address.state(), 2, "UF").toUpperCase(Locale.ROOT);
        if (!STATES.contains(state)) throw new IllegalArgumentException("Informe uma UF válida.");
        String zip = required(address.zipCode(), 9, "CEP");
        if (!zip.matches("[0-9]{5}-?[0-9]{3}"))
            throw new IllegalArgumentException("Informe um CEP com 8 dígitos.");
        zip = zip.replace("-", "");
        zip = zip.substring(0, 5) + "-" + zip.substring(5);
        return new CompanyProfile(null,
            required(data.legalName(), 150, "Razão social"), optional(data.tradeName(), 150, "Nome fantasia"),
            CustomerTaxId.requireValid(data.taxId(), PersonType.PJ),
            optional(data.stateReg(), 20, "Inscrição estadual"),
            optional(data.municipalReg(), 20, "Inscrição municipal"), data.taxRegime(),
            new CompanyProfile.Address(required(address.country(), 50, "País"), state,
                required(address.city(), 100, "Cidade"), required(address.street(), 150, "Logradouro"),
                required(address.neighborhood(), 100, "Bairro"), required(address.number(), 10, "Número"),
                optional(address.complement(), 100, "Complemento"), zip), data.version());
    }

    private String required(String value, int max, String label) {
        String result = optional(value, max, label);
        if (result == null) throw new IllegalArgumentException(label + " é obrigatório(a).");
        return result;
    }

    private String optional(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw new IllegalArgumentException(label + " excede " + max + " caracteres.");
        return result.isEmpty() ? null : result;
    }
}
