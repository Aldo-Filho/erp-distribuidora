package com.pi.erp.purchase.item;

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
@RequestMapping("/purchase-items")
public class PurchaseItemController {

    @Autowired
    private PurchaseItemRepository repository;

    @Autowired
    private PurchaseItemService service;

    @GetMapping
    public ResponseEntity<Page<PurchaseItem>> search(
        PurchaseItemFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(service.search(filter, ApiPagination.pageable(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseItem> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }

    @PostMapping
    public ResponseEntity<PurchaseItem> register(@RequestBody @Valid RequestPurchaseItemDTO data) {
        PurchaseItem purchaseItem = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseItem);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PurchaseItem> update(
        @PathVariable Long id,
        @RequestBody PatchPurchaseItemDTO data
    ) {
        PurchaseItem purchaseItem = service.update(id, data);
        return ResponseEntity.ok(purchaseItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
