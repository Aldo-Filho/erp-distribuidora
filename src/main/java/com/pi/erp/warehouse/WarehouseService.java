package com.pi.erp.warehouse;

import com.pi.erp.warehouse.address.WarehouseAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {
    @Autowired
    private WarehouseRepository repository;

    public List<Warehouse> search(String name) {
        if (name == null || name.isBlank()) {
            return repository.findAll();
        }
        return repository.findByNameContainingIgnoreCase(name);
    }

    public Warehouse register(RequestWarehouseDTO data) {
        if (repository.existsByNameIgnoreCase(data.name())) {
            throw new IllegalArgumentException("This Warehouse name already exists.");
        }
        Warehouse warehouse = new Warehouse(data);
        return repository.save(warehouse);
    }

    public Warehouse update (Long id, PatchWarehouseDTO data) {
        Warehouse warehouse = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found."));

        warehouse.setName(data.description());
        return repository.save(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found."));

        repository.delete(warehouse);
    }
}
