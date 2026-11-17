package com.pi.erp.stock.movement;

import com.pi.erp.api.ApiPagination;
import com.pi.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-movements")
public class StockMovementController {

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private StockMovementService service;

    @GetMapping
    public ResponseEntity<Page<StockMovement>> search(
        StockMovementFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(
            service.search(
                filter,
                ApiPagination.pageable(
                    page,
                    size,
                    Sort.by("occurredAt").descending().and(Sort.by("id").descending())
                )
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockMovement> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
    }

    @PostMapping
    public ResponseEntity<StockMovement> register(
        @RequestBody @Valid RequestStockMovementDTO data
    ) {
        StockMovementService.MovementRegistration registration = service.register(data);
        HttpStatus status = registration.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(registration.movement());
    }
}
