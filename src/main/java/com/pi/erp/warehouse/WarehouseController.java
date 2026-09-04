package com.pi.erp.warehouse;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseRepository repository;
    @Autowired
    private WarehouseService service;

    @GetMapping("/search")
    public List<Warehouse> findAll(@RequestParam(required = false) String name) {
        return service.search(name);
    }

    @PostMapping
    public ResponseEntity<Warehouse> register(@RequestBody @Valid RequestWarehouseDTO data) {
        Warehouse warehouse = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Warehouse> update(
            @PathVariable Long id,
            @RequestBody @Valid PatchWarehouseDTO data
    ) {
        Warehouse warehouse = service.update(id, data);
        return ResponseEntity.ok(warehouse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Warehouse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
