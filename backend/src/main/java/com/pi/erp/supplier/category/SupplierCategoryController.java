package com.pi.erp.supplier.category;

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
@RequestMapping("/supplier/{supplierId}/categories")
public class SupplierCategoryController {

    @Autowired
    private SupplierCategoryService service;

    @GetMapping
    public ResponseEntity<List<SupplierCategory>> search(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.search(supplierId));
    }

    @PostMapping
    public ResponseEntity<SupplierCategory> register(
        @PathVariable Long supplierId,
        @RequestBody @Valid RequestSupplierCategoryDTO data
    ) {
        SupplierCategory supplierCategory = service.register(supplierId, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierCategory);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long supplierId,
        @PathVariable Long categoryId
    ) {
        service.delete(supplierId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
