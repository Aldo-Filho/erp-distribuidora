package com.pi.erp.product.category;

import com.pi.erp.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository repository;

    public List<Category> search(CategoryFilter filter) {
        Specification<Category> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }

        return repository.findAll(spec);
    }

    public Category register(RequestCategoryDTO data) {
        if (repository.existsByNormalizedName(data.name())) {
            throw new IllegalArgumentException("This category already exists.");
        }

        Category category = new Category(data);
        return repository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        repository.delete(category);
    }
}
