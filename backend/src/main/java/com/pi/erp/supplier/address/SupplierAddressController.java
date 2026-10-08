package com.pi.erp.supplier.address;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supplierAddress")
public class SupplierAddressController {

    @Autowired
    private SupplierAddressRepository repository;

    @Autowired
    private SupplierAddressService service;

    @GetMapping("/search")
    public ResponseEntity<List<SupplierAddress>> search(SupplierAddressFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierAddress> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<SupplierAddress> register(
        @RequestBody @Valid RequestSupplierAddressDTO data
    ) {
       SupplierAddress address = service.register(data);
       return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupplierAddress> update(
        @PathVariable Long id,
        @RequestBody @Valid PatchSupplierAddressDTO data
    ) {
        SupplierAddress address = service.update(id, data);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SupplierAddress> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
