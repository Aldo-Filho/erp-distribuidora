package com.pi.erp.fiscal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders/{orderId}/demonstrative-invoice")
public class DemonstrativeInvoiceController {
    @Autowired private DemonstrativeInvoiceService service;
    @GetMapping public ResponseEntity<DemonstrativeInvoice> find(@PathVariable Long orderId) {
        if(orderId<=0) throw new IllegalArgumentException("Venda inválida.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.find(orderId));
    }
}
