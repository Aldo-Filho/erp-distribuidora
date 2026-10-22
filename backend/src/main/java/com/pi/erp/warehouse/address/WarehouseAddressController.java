package com.pi.erp.warehouse.address;

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
@RequestMapping("/warehouseAddress")
public class WarehouseAddressController {

    @Autowired
    private WarehouseAddressRepository repository;

    @Autowired
    private WarehouseAddressService service;

    @GetMapping("/search")
    public ResponseEntity<List<WarehouseAddress>> search(WarehouseAddressFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseAddress> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<WarehouseAddress> register(
        @RequestBody @Valid RequestWarehouseAddressDTO data
    ) {
        WarehouseAddress address = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WarehouseAddress> update(
        @PathVariable Long id,
        @RequestBody PatchWarehouseAddressDTO data
    ) {
        WarehouseAddress address = service.update(id, data);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
