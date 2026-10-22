package com.pi.erp.supplier.address;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierAddressService {

    @Autowired
    private SupplierAddressRepository repository;

    @Autowired
    private SupplierRepository supplierRepository;

    public List<SupplierAddress> search(SupplierAddressFilter filter) {
        Specification<SupplierAddress> spec = Specification.allOf();

        if (filter.supplierId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), filter.supplierId())
            );
        }
        if (filter.addressType() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("addressType"), filter.addressType())
            );
        }
        if (filter.country() != null && !filter.country().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("country")), "%" + filter.country().toLowerCase() + "%")
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
                cb.like(
                    cb.lower(root.get("neighborhood")),
                    "%" + filter.neighborhood().toLowerCase() + "%"
                )
            );
        }
        if (filter.number() != null && !filter.number().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("number")), "%" + filter.number().toLowerCase() + "%")
            );
        }
        if (filter.complement() != null && !filter.complement().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("complement")),
                    "%" + filter.complement().toLowerCase() + "%"
                )
            );
        }
        if (filter.zipCode() != null && !filter.zipCode().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("zipCode")), "%" + filter.zipCode().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public SupplierAddress register(RequestSupplierAddressDTO data) {
        Supplier supplier = findSupplier(data.supplierId());
        String complement = data.complement() != null && data.complement().isBlank()
            ? null
            : data.complement();

        if (repository.existsDuplicateAddress(
            data.supplierId(),
            data.addressType().name(),
            data.country(),
            data.state(),
            data.city(),
            data.street(),
            data.zipCode(),
            data.number(),
            complement
        )) {
            throw new IllegalArgumentException("Supplier address already exists.");
        }

        SupplierAddress address = new SupplierAddress(data, supplier);
        address.setComplement(complement);
        return repository.save(address);
    }

    @Transactional
    public SupplierAddress update(Long id, PatchSupplierAddressDTO data) {
        SupplierAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier address not found."));

        if (data.country() != null && data.country().isBlank()) {
            throw new IllegalArgumentException("Country is required.");
        }
        if (data.state() != null && data.state().isBlank()) {
            throw new IllegalArgumentException("State is required.");
        }
        if (data.city() != null && data.city().isBlank()) {
            throw new IllegalArgumentException("City is required.");
        }
        if (data.street() != null && data.street().isBlank()) {
            throw new IllegalArgumentException("Street is required.");
        }
        if (data.neighborhood() != null && data.neighborhood().isBlank()) {
            throw new IllegalArgumentException("Neighborhood is required.");
        }
        if (data.number() != null && data.number().isBlank()) {
            throw new IllegalArgumentException("Number is required.");
        }
        if (data.zipCode() != null && data.zipCode().isBlank()) {
            throw new IllegalArgumentException("Zip code is required.");
        }

        AddressType addressType = data.addressType() != null
            ? data.addressType()
            : address.getAddressType();
        String country = data.country() != null ? data.country() : address.getCountry();
        String state = data.state() != null ? data.state() : address.getState();
        String city = data.city() != null ? data.city() : address.getCity();
        String street = data.street() != null ? data.street() : address.getStreet();
        String zipCode = data.zipCode() != null ? data.zipCode() : address.getZipCode();
        String number = data.number() != null ? data.number() : address.getNumber();
        String complement = address.getComplement();

        if (data.complement() != null) {
            complement = data.complement().isBlank() ? null : data.complement();
        }

        if (repository.existsDuplicateAddressAndIdNot(
            address.getSupplier().getId(),
            addressType != null ? addressType.name() : null,
            country,
            state,
            city,
            street,
            zipCode,
            number,
            complement,
            id
        )) {
            throw new IllegalArgumentException("Supplier address already exists.");
        }

        address.setCountry(country);
        address.setState(state);
        address.setCity(city);
        address.setStreet(street);
        if (data.neighborhood() != null) {
            address.setNeighborhood(data.neighborhood());
        }
        address.setNumber(number);
        address.setComplement(complement);
        address.setZipCode(zipCode);
        address.setAddressType(addressType);

        return repository.save(address);
    }

    @Transactional
    public void delete(Long id) {
        SupplierAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier address not found."));

        Supplier supplier = address.getSupplier();
        supplier.getSupplierAddresses().remove(address);
        repository.delete(address);
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }
}
