package com.pi.erp.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company/profile")
public class CompanyProfileController {
    @Autowired private CompanyProfileService service;

    public record SaveProfile(
        String legalName, String tradeName, String taxId, String stateReg, String municipalReg,
        @NotNull CompanyProfile.TaxRegime taxRegime, @NotNull CompanyProfile.Address address,
        @NotNull @Min(0) Long version
    ) {}

    @GetMapping
    public ResponseEntity<CompanyProfile> find() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find());
    }

    @PutMapping
    public ResponseEntity<CompanyProfile> save(@RequestBody @Valid SaveProfile data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.save(data));
    }
}
