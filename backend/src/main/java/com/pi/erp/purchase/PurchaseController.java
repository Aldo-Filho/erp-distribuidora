package com.pi.erp.purchase;

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
@RequestMapping("/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseRepository repository;

    @Autowired
    private PurchaseService service;

    @GetMapping("/search")
    public ResponseEntity<List<Purchase>> search(PurchaseFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<Purchase> register(
        @RequestBody @Valid RequestPurchaseDTO data
    ) {
        Purchase purchase = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Purchase> update(
        @PathVariable Long id,
        @RequestBody PatchPurchaseDTO data
    ) {
        Purchase purchase = service.update(id, data);
        return ResponseEntity.ok(purchase);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
