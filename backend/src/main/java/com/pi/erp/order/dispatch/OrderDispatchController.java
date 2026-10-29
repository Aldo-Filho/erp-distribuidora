package com.pi.erp.order.dispatch;

import com.pi.erp.order.Order;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderDispatchController {

    @Autowired
    private OrderDispatchService service;

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<Order> dispatch(
        @PathVariable Long id,
        @RequestBody @Valid RequestOrderDispatchDTO data
    ) {
        return ResponseEntity.ok(service.dispatch(id, data));
    }
}
