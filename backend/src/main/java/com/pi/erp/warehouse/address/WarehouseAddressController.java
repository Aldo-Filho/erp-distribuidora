package com.pi.erp.warehouse.address;

import com.pi.erp.api.ApiPagination;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
@RequestMapping("/warehouse-addresses")
public class WarehouseAddressController {

    @Autowired
    private WarehouseAddressRepository repository;

    @Autowired
    private WarehouseAddressService service;

    @GetMapping
    public ResponseEntity<Page<WarehouseAddress>> search(
        WarehouseAddressFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(service.search(filter, ApiPagination.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseAddress> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
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
