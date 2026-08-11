package com.pi.erp.product;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.product.category.Category;
import com.pi.erp.product.category.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository repository;

    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public List<Product> search(ProductFilter filter) {
        Specification<Product> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()){
            spec = spec.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("name")),
                                    "%" + filter.name().toLowerCase() + "%"
                            )
            );
        }
        if (filter.sku() != null && !filter.sku().isBlank()) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(root.get("sku"), filter.sku())
            );
        }
        if (filter.categoryId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("category").get("id"),
                                    filter.categoryId()
                            )
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("brand").get("id"),
                                    filter.brandId()
                            )
            );
        }
        return repository.findAll(spec);
    }

    public Product register(RequestProductDTO data) {

        if (repository.existsBySku(data.sku())) {
            throw new IllegalArgumentException("Product already exists.");
        }

        Brand brand = brandRepository.findById(data.brandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found."));

        Category category = null;
        if (data.categoryId() != null) {
            category = categoryRepository.findById(data.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        }

        Product product = new Product(data, brand, category);
        return repository.save(product);
    }
}
