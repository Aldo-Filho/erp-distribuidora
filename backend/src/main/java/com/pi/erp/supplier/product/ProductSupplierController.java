package com.pi.erp.supplier.product;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productSupplier")
public class ProductSupplierController {

    @Autowired
    private ProductSupplierRepository repository;

    @Autowired
    private ProductSupplierService service;

    @GetMapping("/search")
    public ResponseEntity<List<ProductSupplier>> search(@Valid ProductSupplierFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSupplier> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<ProductSupplier> register(
        @RequestBody @Valid RequestProductSupplierDTO data
    ) {
        ProductSupplier productSupplier = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(productSupplier);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductSupplier> update(
        @PathVariable Long id,
        @RequestBody @Valid PatchProductSupplierDTO data
    ) {
        ProductSupplier productSupplier = service.update(id, data);
        return ResponseEntity.ok(productSupplier);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
