package com.pi.erp.product;

import com.pi.erp.exception.ResourceNotFoundException;
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
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductService service;

    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(ProductFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findOne(@PathVariable Long id) {
        Product product = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> register(
        @RequestBody @Valid RequestProductDTO data
    ) {
        Product product = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Product> update(
        @PathVariable Long id,
        @RequestBody @Valid PatchProductDTO data
    ) {
        Product product = service.update(id, data);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
