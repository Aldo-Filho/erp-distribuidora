package com.pi.erp.warehouse;

import com.pi.erp.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository repository;

    public Page<Warehouse> search(WarehouseFilter filter, Pageable pageable) {
        Specification<Warehouse> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec, pageable);
    }

    public Warehouse register(RequestWarehouseDTO data) {
        if (repository.existsByNormalizedName(data.name())) {
            throw new IllegalArgumentException("This warehouse name already exists.");
        }

        Warehouse warehouse = new Warehouse(data);
        warehouse.setName(data.name().trim());
        return repository.save(warehouse);
    }

    public Warehouse update(Long id, PatchWarehouseDTO data) {
        Warehouse warehouse = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));

        String name = null;
        if (data.name() != null) {
            name = data.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Warehouse name is required.");
            }
            if (repository.existsByNormalizedNameAndIdNot(name, id)) {
                throw new IllegalArgumentException("This warehouse name already exists.");
            }
        }

        String description = data.description() == null ? null : data.description().trim();

        if (name != null) {
            warehouse.setName(name);
        }
        if (description != null) {
            warehouse.setDescription(description.isEmpty() ? null : description);
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
