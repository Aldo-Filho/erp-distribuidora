package com.pi.erp.price;

import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/price-table")
public class PriceTableController {
    @Autowired
    private PriceTableRepository repository;

    @Autowired
    private PriceTableService service;

    @GetMapping("/search")
    public ResponseEntity<List<PriceTable>> search(PriceTableFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceTable> findOne(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<PriceTable> register(@RequestBody @Valid RequestPriceTableDTO data) {
        PriceTable priceTable = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(priceTable);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PriceTable> update(
            @PathVariable Long id,
            @RequestBody PatchPriceTableDTO data
    ) {
        PriceTable priceTable = service.update(id, data);
        return ResponseEntity.ok(priceTable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
