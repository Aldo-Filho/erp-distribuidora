package com.pi.erp.customer.address;

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
@RequestMapping("/customerAddress")
public class CustomerAddressController {
    @Autowired
    private CustomerAddressService service;

    @GetMapping("/search")
    public ResponseEntity<List<CustomerAddress>> search(CustomerAddressFilter filter) {
        return ResponseEntity.ok(service.search(filter));
    }

    @PostMapping
    public ResponseEntity<CustomerAddress> register(
            @RequestBody @Valid RequestCustomerAddressDTO data
    ) {
        CustomerAddress address = service.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerAddress> update(
            @PathVariable Long id,
            @RequestBody PatchCustomerAddressDTO data
    ) {
        CustomerAddress address = service.update(id, data);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
