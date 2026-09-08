package com.pi.erp.stock;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stockItem")
public class StockItemController {
    @Autowired
    private StockItemRepository repository;
    @Autowired
    private StockItemService service;

    @GetMapping("/search")
    public ResponseEntity<List<StockItem>> search(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId
    ) {
        return ResponseEntity.ok(service.search(warehouseId, productId));
    }

    @PostMapping
    public ResponseEntity<StockItem> register(@RequestBody @Valid RequestStockItemDTO data) {
        StockItem stockItem = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(stockItem);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StockItem> update(
            @PathVariable Long id,
            @RequestBody PatchStockItemDTO data
    ) {
        StockItem stockItem = service.update(id, data);
        return ResponseEntity.ok(stockItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

