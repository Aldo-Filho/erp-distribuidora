package com.pi.erp.customer.address;

import com.pi.erp.customer.Customer;
import com.pi.erp.customer.CustomerRepository;
import com.pi.erp.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                cb.equal(root.get("customer").get("id"), filter.customerId())
            );
        }
        if (filter.addressType() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("addressType"), filter.addressType())
            );
        }
        if (filter.state() != null && !filter.state().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("state")), "%" + filter.state().toLowerCase() + "%")
            );
        }
        if (filter.city() != null && !filter.city().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("city")), "%" + filter.city().toLowerCase() + "%")
            );
        }
        if (filter.street() != null && !filter.street().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("street")), "%" + filter.street().toLowerCase() + "%")
            );
        }
        if (filter.neighborhood() != null && !filter.neighborhood().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("neighborhood")), "%" + filter.neighborhood().toLowerCase() + "%")
            );
        }
        if (filter.number() != null && !filter.number().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("number")), "%" + filter.number().toLowerCase() + "%")
            );
        }
        if (filter.complement() != null && !filter.complement().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("complement")), "%" + filter.complement().toLowerCase() + "%")
            );
        }
        if (filter.zipCode() != null && !filter.zipCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("zipCode")), "%" + filter.zipCode().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec);
    }

    public CustomerAddress register(RequestCustomerAddressDTO data) {
        Customer customer = findCustomer(data.customerId());

        if (repository.existsByCustomerIdAndAddressType(
            data.customerId(),
            data.addressType()
        )) {
            throw new IllegalArgumentException("Customer already has an address of this type.");
        }

        CustomerAddress address = new CustomerAddress(data, customer);
        return repository.save(address);
    }

    public CustomerAddress update(Long id, PatchCustomerAddressDTO data) {
        CustomerAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer address not found."));

        AddressType addressType = data.addressType() != null
            ? data.addressType()
            : address.getAddressType();

        if (repository.existsByCustomerIdAndAddressTypeAndIdNot(
            address.getCustomer().getId(),
            addressType,
            id
        )) {
            throw new IllegalArgumentException(
                "Customer already has an address of this type."
            );
        }

        address.setAddressType(addressType);

        if (data.state() != null) {
            if (data.state().isBlank()) {
                throw new IllegalArgumentException("State is required.");
            }
            address.setState(data.state());
        }
        if (data.city() != null) {
            if (data.city().isBlank()) {
                throw new IllegalArgumentException("City is required.");
            }
            address.setCity(data.city());
        }
        if (data.street() != null) {
            if (data.street().isBlank()) {
                throw new IllegalArgumentException("Street is required.");
            }
            address.setStreet(data.street());
        }
        if (data.neighborhood() != null) {
            if (data.neighborhood().isBlank()) {
                throw new IllegalArgumentException("Neighborhood is required.");
            }
            address.setNeighborhood(data.neighborhood());
        }
        if (data.number() != null) {
            if (data.number().isBlank()) {
                throw new IllegalArgumentException("Number is required.");
            }
            address.setNumber(data.number());
        }
        if (data.complement() != null) {
            if (data.complement().isBlank()) {
                throw new IllegalArgumentException("Complement is required.");
            }
            address.setComplement(data.complement());
        }
        if (data.zipCode() != null) {
            if (data.zipCode().isBlank()) {
                throw new IllegalArgumentException("Zip code is required.");
            }
            address.setZipCode(data.zipCode());
        }

        return repository.save(address);
    }

    @Transactional
    public void delete(Long id) {
        CustomerAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer address not found."));

        repository.delete(address);
    }

    private Customer findCustomer(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is Required.");
        }

        return customerRepository
            .findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }
}
