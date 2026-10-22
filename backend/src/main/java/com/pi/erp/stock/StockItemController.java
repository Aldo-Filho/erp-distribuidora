package com.pi.erp.stock;

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
@RequestMapping("/stockItem")
public class StockItemController {

    @Autowired
    private StockItemService service;

    @GetMapping("/search")
    public ResponseEntity<List<StockItem>> search(StockItemFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @PostMapping
    public ResponseEntity<StockItem> register(
        @RequestBody @Valid RequestStockItemDTO data
    ) {
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
