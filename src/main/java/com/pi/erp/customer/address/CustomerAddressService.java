package com.pi.erp.customer.address;

import com.pi.erp.customer.Customer;
import com.pi.erp.customer.CustomerRepository;
import com.pi.erp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerAddressService {
    @Autowired
    private CustomerAddressRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    public List<CustomerAddress> search(CustomerAddressFilter filter) {
        Specification<CustomerAddress> spec = Specification.allOf();

        if (filter.customerId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("customer").get("id"), filter.customerId()));
        }
        if (filter.addressType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("addressType"), filter.addressType()));
        }
        if (filter.state() != null && !filter.state().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("state")),
                            "%" + filter.state().toLowerCase() + "%"));
        }
        if (filter.city() != null && !filter.city().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("city")),
                            "%" + filter.city().toLowerCase() + "%"));
        }
        if (filter.street() != null && !filter.street().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("street")),
                            "%" + filter.street().toLowerCase() + "%"));
        }
        if (filter.neighborhood() != null && !filter.neighborhood().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("neighborhood")),
                            "%" + filter.neighborhood().toLowerCase() + "%"));
        }
        if (filter.number() != null && !filter.number().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("number")),
                            "%" + filter.number().toLowerCase() + "%"));
        }
        if (filter.complement() != null && !filter.complement().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("complement")),
                            "%" + filter.complement().toLowerCase() + "%"));
        }
        if (filter.zipCode() != null && !filter.zipCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("zipCode")),
                            "%" + filter.zipCode().toLowerCase() + "%"));
        }

        return repository.findAll(spec);
    }

    public CustomerAddress register(RequestCustomerAddressDTO data) {
        Customer customer = findCustomer(data.customerId());
        return repository.save(new CustomerAddress(data, customer));
    }

    public CustomerAddress update(Long id, PatchCustomerAddressDTO data) {
        CustomerAddress address = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer address not found."));

        if (data.customerId() != null) {
            address.setCustomer(findCustomer(data.customerId()));
        }
        if (data.addressType() != null) {
            address.setAddressType(data.addressType());
        }
        if (data.state() != null && !data.state().isBlank()) {
            address.setState(data.state());
        }
        if (data.city() != null && !data.city().isBlank()) {
            address.setCity(data.city());
        }
        if (data.street() != null && !data.street().isBlank()) {
            address.setStreet(data.street());
        }
        if (data.neighborhood() != null && !data.neighborhood().isBlank()) {
            address.setNeighborhood(data.neighborhood());
        }
        if (data.number() != null && !data.number().isBlank()) {
            address.setNumber(data.number());
        }
        if (data.complement() != null && !data.complement().isBlank()) {
            address.setComplement(data.complement());
        }
        if (data.zipCode() != null && !data.zipCode().isBlank()) {
            address.setZipCode(data.zipCode());
        }

        return repository.save(address);
    }

    @Transactional
    public void delete(Long id) {
        CustomerAddress address = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer address not found."));

        repository.delete(address);
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }
}
