package com.pi.erp.purchase.receipt;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase")
public class PurchaseReceiptController {

    @Autowired
    private PurchaseReceiptService service;

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseReceipt> register(
        @PathVariable Long id,
        @RequestBody @Valid RequestPurchaseReceiptDTO data
    ) {
        PurchaseReceiptService.ReceiptRegistration registration = service.register(id, data);
        HttpStatus status = registration.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(registration.receipt());
    }

    @GetMapping("/{id}/receipts")
    public ResponseEntity<List<PurchaseReceipt>> search(@PathVariable Long id) {
        return ResponseEntity.ok(service.search(id));
    }

    @GetMapping("/{purchaseId}/receipts/{id}")
    public ResponseEntity<PurchaseReceipt> findOne(
        @PathVariable Long purchaseId,
        @PathVariable Long id
    ) {
        return service.findOne(purchaseId, id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
