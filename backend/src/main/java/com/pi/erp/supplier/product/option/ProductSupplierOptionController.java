package com.pi.erp.supplier.product.option;

import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productSupplierOption")
public class ProductSupplierOptionController {

    @Autowired
    private ProductSupplierOptionService service;

    @Autowired
    private ProductSupplierOptionRepository repository;

    @GetMapping("/search")
    public ResponseEntity<Page<ProductSupplierOption>> search(
        ProductSupplierOptionFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by("name", "id")))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSupplierOption> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(
            repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found."))
        );
    }

    @PostMapping
    public ResponseEntity<ProductSupplierOption> register(
        @RequestBody @Valid RequestProductSupplierOptionDTO data
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductSupplierOption> update(
        @PathVariable Long id,
        @RequestBody PatchProductSupplierOptionDTO data
    ) {
        return ResponseEntity.ok(service.update(id, data));
    }
}
