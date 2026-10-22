package com.pi.erp.order.item;

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
@RequestMapping("/orderItem")
public class OrderItemController {

    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private OrderItemService service;

    @GetMapping("/search")
    public ResponseEntity<List<OrderItem>> search(OrderItemFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderItem> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<OrderItem> register(
        @RequestBody @Valid RequestOrderItemDTO data
    ) {
        OrderItem orderItem = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItem);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderItem> update(
        @PathVariable Long id,
        @RequestBody PatchOrderItemDTO data
    ) {
        OrderItem orderItem = service.update(id, data);
        return ResponseEntity.ok(orderItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
