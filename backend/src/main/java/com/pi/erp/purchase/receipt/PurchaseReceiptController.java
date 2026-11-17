package com.pi.erp.purchase.receipt;

import com.pi.erp.api.ApiPagination;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchases")
public class PurchaseReceiptController {

    @Autowired
    private PurchaseReceiptService service;

    @PostMapping("/{id}/receipts")
    public ResponseEntity<PurchaseReceipt> register(
        @PathVariable Long id,
        @RequestBody @Valid RequestPurchaseReceiptDTO data
    ) {
        PurchaseReceiptService.ReceiptRegistration registration = service.register(id, data);
        HttpStatus status = registration.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(registration.receipt());
    }

    @GetMapping("/{id}/receipts")
    public ResponseEntity<Page<PurchaseReceipt>> search(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(service.search(id, ApiPagination.pageable(page, size)));
    }

    @GetMapping("/{purchaseId}/receipts/{id}")
    public ResponseEntity<PurchaseReceipt> findOne(
        @PathVariable Long purchaseId,
        @PathVariable Long id
    ) {
        return service
            .findOne(purchaseId, id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }
}
