package com.pi.erp.product.brand;

import com.pi.erp.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    @Autowired
    private BrandRepository repository;

    public List<Brand> search(BrandFilter filter) {
        Specification<Brand> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec);
    }

    public Brand register(RequestBrandDTO data) {
        if (repository.existsByNormalizedName(data.name())) {
            throw new IllegalArgumentException("This brand already exists.");
        }

        Brand brand = new Brand(data);
        return repository.save(brand);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));

        repository.delete(brand);
    }
}
