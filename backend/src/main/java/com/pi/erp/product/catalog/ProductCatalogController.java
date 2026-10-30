package com.pi.erp.product.catalog;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
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
@RequestMapping("/productCatalog")
public class ProductCatalogController {

    @Autowired
    private ProductCatalogService service;

    @Autowired
    private ProductCatalogRepository repository;

    @GetMapping("/search")
    public ResponseEntity<Page<ProductCatalog>> search(
        ProductCatalogFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        validatePage(page, size);
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by("name", "id")))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCatalog> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(
            repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product catalog not found."))
        );
    }

    @PostMapping
    public ResponseEntity<ProductCatalog> register(
        @RequestBody @Valid RequestProductCatalogDTO data
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductCatalog> update(
        @PathVariable Long id,
        @RequestBody PatchProductCatalogDTO data
    ) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @GetMapping("/{id}/items/search")
    public ResponseEntity<Page<Product>> search(
        @PathVariable Long id,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        validatePage(page, size);
        return ResponseEntity.ok(
            service.searchItems(id, query, active, PageRequest.of(page, size, Sort.by("sku", "id")))
        );
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Product> register(
        @PathVariable Long id,
        @RequestBody @Valid RequestCatalogItemDTO data
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerItem(id, data));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid product pagination limits.");
        }
    }
}
