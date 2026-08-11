package com.pi.erp.product;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductService service;

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<Product> register(@RequestBody @Valid RequestProductDTO data) {
        Product product = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

//    @PatchMapping
//    public ResponseEntity<Product> update(@RequestBody @Valid RequestProductDTO data) {
//
//        return ResponseEntity.ok(update);
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Product> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
