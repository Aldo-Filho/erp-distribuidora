package com.pi.erp.customer;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private CustomerService service;

    @GetMapping("/search")
    public ResponseEntity<Page<Customer>> search(
        CustomerFilter filter,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid customer pagination limits.");
        }
        return ResponseEntity.ok(
            service.search(
                filter,
                PageRequest.of(page, size, Sort.by("legalName").ascending().and(Sort.by("id")))
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> findOne(@PathVariable Long id) {
        return repository
            .findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<Customer> register(
        @RequestBody @Valid RequestCustomerDTO data
    ) {
        Customer customer = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Customer> update(
        @PathVariable Long id,
        @RequestBody PatchCustomerDTO data
    ) {
        Customer customer = service.update(id, data);
        return ResponseEntity.ok(customer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
