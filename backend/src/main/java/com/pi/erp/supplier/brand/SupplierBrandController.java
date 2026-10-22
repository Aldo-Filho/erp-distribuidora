package com.pi.erp.supplier.brand;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/supplier/{supplierId}/brands")
public class SupplierBrandController {

    @Autowired
    private SupplierBrandService service;

    @GetMapping
    public ResponseEntity<List<SupplierBrand>> search(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.search(supplierId));
    }

    @PostMapping
    public ResponseEntity<SupplierBrand> register(
        @PathVariable Long supplierId,
        @RequestBody @Valid RequestSupplierBrandDTO data
    ) {
        SupplierBrand supplierBrand = service.register(supplierId, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierBrand);
    }

    @DeleteMapping("/{brandId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long supplierId,
        @PathVariable Long brandId
    ) {
        service.delete(supplierId, brandId);
        return ResponseEntity.noContent().build();
    }
}
