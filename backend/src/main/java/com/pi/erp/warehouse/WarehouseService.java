package com.pi.erp.warehouse;

import com.pi.erp.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository repository;

    public List<Warehouse> search(WarehouseFilter filter) {
        Specification<Warehouse> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec);
    }

    public Warehouse register(RequestWarehouseDTO data) {
        if (repository.existsByNormalizedName(data.name())) {
            throw new IllegalArgumentException("This warehouse name already exists.");
        }

        Warehouse warehouse = new Warehouse(data);
        return repository.save(warehouse);
    }

    public Warehouse update(Long id, PatchWarehouseDTO data) {
        Warehouse warehouse = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        if (data.description() != null) {
            warehouse.setDescription(data.description().isBlank() ? null : data.description());
        }

        return repository.save(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        repository.delete(warehouse);
    }
}
