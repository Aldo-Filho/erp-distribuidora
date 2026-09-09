package com.pi.erp.customer;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    @Autowired
    private CustomerRepository repository;

    @Autowired
    private CustomerService service;

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> search(CustomerFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> findOne(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<Customer> register(@RequestBody @Valid RequestCustomerDTO data) {
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
