package com.pi.erp.stock.movement;

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
@RequestMapping("/stockMovement")
public class StockMovementController {

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private StockMovementService service;

    @GetMapping("/search")
    public ResponseEntity<List<StockMovement>> search(StockMovementFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockMovement> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<StockMovement> register(
        @RequestBody @Valid RequestStockMovementDTO data
    ) {
        StockMovement stockMovement = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(stockMovement);
    }
}
