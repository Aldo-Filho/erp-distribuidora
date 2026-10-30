package com.pi.erp.stock;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/stockItem")
public class StockItemController {

    @Autowired
    private StockItemService service;

    @GetMapping("/search")
    public ResponseEntity<Page<StockItem>> search(
        StockItemFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid stockitem pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(filter, PageRequest.of(page, size, Sort.by("id").ascending()))
        );
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
