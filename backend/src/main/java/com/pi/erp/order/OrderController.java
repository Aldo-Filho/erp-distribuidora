package com.pi.erp.order;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderService service;

    @GetMapping("/search")
    public ResponseEntity<Page<Order>> search(
        OrderFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid order pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by("id").ascending()))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/price")
    public ResponseEntity<BigDecimal> resolvePrice(
        @PathVariable Long id,
        @RequestParam Long productId,
        @RequestParam(required = false) Long presentationId
    ) {
        return ResponseEntity.ok(service.resolvePrice(id, productId, presentationId));
    }

    @PostMapping
    public ResponseEntity<Order> register(@RequestBody @Valid RequestOrderDTO data) {
        Order order = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody PatchOrderDTO data) {
        Order order = service.update(id, data);
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
