package com.pi.erp.warehouse.address;

import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseAddressService {
    @Autowired
    private WarehouseAddressRepository repository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    public List<WarehouseAddress> search(WarehouseAddressFilter filter) {
        Specification<WarehouseAddress> spec = Specification.allOf();

        if (filter.warehouseId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("warehouse").get("id"),
                                    filter.warehouseId()
                            )
            );
        }
        if (filter.state() != null && !filter.state().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("state")),
                                    "%" + filter.state().toLowerCase() + "%"
                            )
            );
        }
        if (filter.city() != null && !filter.city().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("city")),
                                    "%" + filter.city().toLowerCase() + "%"
                            )
            );
        }
        if (filter.street() != null && !filter.street().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("street")),
                                    "%" + filter.street().toLowerCase() + "%"
                            )
            );
        }
        if (filter.neighborhood() != null && !filter.neighborhood().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("neighborhood")),
                                    "%" + filter.neighborhood().toLowerCase() + "%"
                            )
            );
        }
        if (filter.number() != null && !filter.number().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("number")),
                                    "%" + filter.number().toLowerCase() + "%"
                            )
            );
        }
        if (filter.complement() != null && !filter.complement().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("complement")),
                                    "%" + filter.complement().toLowerCase() + "%"
                            )
            );
        }
        if (filter.zipCode() != null && !filter.zipCode().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("zipCode")),
                                    "%" + filter.zipCode().toLowerCase() + "%"
                            )
            );
        }
        return repository.findAll(spec);
    }

    public WarehouseAddress register(RequestWarehouseAddressDTO data) {
        if(repository.existsByZipCodeAndNumberAndComplementIgnoreCase(
                data.zipCode(),
                data.number(),
                data.complement())) {
            throw new IllegalArgumentException("Address already exists");
        }

        Warehouse warehouse = warehouseRepository.findById(data.warehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        WarehouseAddress address = new WarehouseAddress(data, warehouse);
        return repository.save(address);
    }

    public WarehouseAddress update(Long id, PatchWarehouseAddressDTO data) {
        WarehouseAddress address = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse Address not found"));

        if (data.warehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(data.warehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
            address.setWarehouse(warehouse);
        }

        if (data.state() != null) {
            address.setState(data.state());
        }

        if (data.city() != null) {
            address.setCity(data.city());
        }

        if (data.street() != null) {
            address.setStreet(data.street());
        }

        if (data.neighborhood() != null) {
            address.setNeighborhood(data.neighborhood());
        }

        if (data.number() != null) {
            address.setNumber(data.number());
        }

        if (data.complement() != null) {
            address.setComplement(data.complement());
        }

        if (data.zipCode() != null) {
            address.setZipCode(data.zipCode());
        }

        return repository.save(address);
    }

    @Transactional
    public void delete(Long id) {
        WarehouseAddress address = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse address not found."));

        Warehouse warehouse = address.getWarehouse();
        warehouse.setWarehouseAddress(null); // desfaz o lado do cascade/orphanRemoval

        repository.delete(address);
    }
}
