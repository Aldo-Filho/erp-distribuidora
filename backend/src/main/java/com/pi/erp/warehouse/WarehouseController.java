package com.pi.erp.warehouse;

import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    @Autowired
    private WarehouseService service;

    @Autowired
    private WarehouseRepository repository;

    @GetMapping
    public ResponseEntity<Page<Warehouse>> search(
        WarehouseFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid warehouse pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(
                filter,
                PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")))
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }

    @PostMapping
    public ResponseEntity<Warehouse> register(@RequestBody @Valid RequestWarehouseDTO data) {
        Warehouse warehouse = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Warehouse> update(
        @PathVariable Long id,
        @RequestBody PatchWarehouseDTO data
    ) {
        Warehouse warehouse = service.update(id, data);
        return ResponseEntity.ok(warehouse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
