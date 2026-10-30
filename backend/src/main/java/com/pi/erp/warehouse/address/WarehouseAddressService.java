package com.pi.erp.warehouse.address;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseAddressService {

    @Autowired
    private WarehouseAddressRepository repository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    public List<WarehouseAddress> search(WarehouseAddressFilter filter) {
        Specification<WarehouseAddress> spec = Specification.allOf();

        if (filter.warehouseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), filter.warehouseId())
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

    public WarehouseAddress register(RequestWarehouseAddressDTO data) {
        data = new RequestWarehouseAddressDTO(
            data.warehouseId(),
            normalizeText(data.state()),
            normalizeText(data.city()),
            normalizeText(data.street()),
            normalizeText(data.neighborhood()),
            normalizeText(data.number()),
            normalizeText(data.complement()),
            normalizeText(data.zipCode())
        );
        Warehouse warehouse = findWarehouse(data.warehouseId());

        if (repository.existsByWarehouseId(data.warehouseId())) {
            throw new IllegalArgumentException("Warehouse already has an address.");
        }

        if (repository.existsDuplicateAddress(
            data.state(),
            data.city(),
            data.street(),
            data.zipCode(),
            data.number(),
            data.complement()
        )) {
            throw new IllegalArgumentException("Warehouse address already exists.");
        }

        WarehouseAddress address = new WarehouseAddress(data, warehouse);
        return repository.save(address);
    }

    public WarehouseAddress update(Long id, PatchWarehouseAddressDTO data) {
        data = new PatchWarehouseAddressDTO(
            data.warehouseId(),
            normalizeText(data.state()),
            normalizeText(data.city()),
            normalizeText(data.street()),
            normalizeText(data.neighborhood()),
            normalizeText(data.number()),
            normalizeText(data.complement()),
            normalizeText(data.zipCode())
        );
        WarehouseAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse address not found."));

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
        if (data.complement() != null && data.complement().isBlank()) {
            throw new IllegalArgumentException("Complement is required.");
        }
        if (data.zipCode() != null && data.zipCode().isBlank()) {
            throw new IllegalArgumentException("Zip code is required.");
        }

        Warehouse warehouse = address.getWarehouse();

        if (data.warehouseId() != null) {
            warehouse = findWarehouse(data.warehouseId());
        }

        String state = data.state() != null ? data.state() : address.getState();
        String city = data.city() != null ? data.city() : address.getCity();
        String street = data.street() != null ? data.street() : address.getStreet();
        String zipCode = data.zipCode() != null ? data.zipCode() : address.getZipCode();
        String number = data.number() != null ? data.number() : address.getNumber();
        String complement = data.complement() != null
            ? data.complement()
            : address.getComplement();

        if (repository.existsByWarehouseIdAndIdNot(warehouse.getId(), id)) {
            throw new IllegalArgumentException("Warehouse already has an address.");
        }
        if (repository.existsDuplicateAddressAndIdNot(
            state,
            city,
            street,
            zipCode,
            number,
            complement,
            id
        )) {
            throw new IllegalArgumentException("Warehouse address already exists.");
        }

        address.setWarehouse(warehouse);
        address.setState(state);
        address.setCity(city);
        address.setStreet(street);

        if (data.neighborhood() != null) {
            address.setNeighborhood(data.neighborhood());
        }
        address.setNumber(number);
        address.setComplement(complement);
        address.setZipCode(zipCode);

        return repository.save(address);
    }

    @Transactional
    public void delete(Long id) {
        WarehouseAddress address = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse address not found."));

        Warehouse warehouse = address.getWarehouse();
        warehouse.setWarehouseAddress(null);

        repository.delete(address);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private Warehouse findWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required.");
        }

        return warehouseRepository
            .findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
    }
}
