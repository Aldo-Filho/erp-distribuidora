package com.pi.erp.price.item;

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
@RequestMapping("/priceTableItem")
public class PriceTableItemController {

    @Autowired
    private PriceTableItemRepository repository;

    @Autowired
    private PriceTableItemService service;

    @GetMapping("/search")
    public ResponseEntity<List<PriceTableItem>> search(PriceTableItemFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceTableItem> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<PriceTableItem> register(
        @RequestBody @Valid RequestPriceTableItemDTO data
    ) {
        PriceTableItem priceTableItem = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(priceTableItem);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PriceTableItem> update(
        @PathVariable Long id,
        @RequestBody PatchPriceTableItemDTO data
    ) {
        PriceTableItem priceTableItem = service.update(id, data);
        return ResponseEntity.ok(priceTableItem);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
