package com.pi.erp.purchase;

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
@RequestMapping("/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseRepository repository;

    @Autowired
    private PurchaseService service;

    @GetMapping
    public ResponseEntity<Page<Purchase>> search(
        PurchaseFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid purchase pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by("id").descending()))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Purchase> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }

    @PostMapping
    public ResponseEntity<Purchase> register(@RequestBody @Valid RequestPurchaseDTO data) {
        Purchase purchase = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Purchase> update(
        @PathVariable Long id,
        @RequestBody PatchPurchaseDTO data
    ) {
        if (data.status() != null) {
            throw new IllegalArgumentException(
                "Use submissions, confirmations or cancellations to change purchase status."
            );
        }
        Purchase purchase = service.update(id, data);
        return ResponseEntity.ok(purchase);
    }

    @PostMapping("/{id}/submissions")
    public ResponseEntity<Purchase> submit(@PathVariable Long id) {
        return ResponseEntity.ok(
            service.update(id, new PatchPurchaseDTO(null, null, null, null, PurchaseStatus.SENT))
        );
    }

    @PostMapping("/{id}/confirmations")
    public ResponseEntity<Purchase> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(
            service.update(
                id,
                new PatchPurchaseDTO(null, null, null, null, PurchaseStatus.CONFIRMED)
            )
        );
    }

    @PostMapping("/{id}/cancellations")
    public ResponseEntity<Purchase> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(
            service.update(
                id,
                new PatchPurchaseDTO(null, null, null, null, PurchaseStatus.CANCELED)
            )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
