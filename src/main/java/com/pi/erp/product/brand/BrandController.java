package com.pi.erp.product.brand;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brand")
public class BrandController {
    @Autowired
    private BrandRepository repository;

    @Autowired
    private BrandService service;

    @GetMapping
    public List<Brand> findAll(@RequestParam(required = false) String name) {
        return service.search(name);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Brand> findOne(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<Brand> register(@RequestBody @Valid RequestBrandDTO data) {
        Brand brand = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(brand);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
