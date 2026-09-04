package com.pi.erp.product;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.product.category.Category;
import com.pi.erp.product.category.CategoryRepository;
import com.pi.erp.warehouse.Warehouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (filter.active() != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(root.get("active"), filter.active())
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

    public Product update(Long id, PatchProductDTO data) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (data.name() != null && !data.name().isBlank()) {
            product.setName(data.name());
        }

        if (data.brandId() != null) {
            Brand brand = brandRepository.findById(data.brandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
        }

        if (data.categoryId() != null) {
            Category category = categoryRepository.findById(data.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        if (data.cost() != null) {
            product.setCost(data.cost());
        }

        if (data.price() != null) {
            product.setPrice(data.price());
        }

        if (data.weightKg() != null) {
            product.setWeightKg(data.weightKg());
        }

        if (data.color() != null && !data.color().isBlank()) {
            product.setColor(data.color());
        }

        if (data.dimensionX() != null) {
            product.setDimensionX(data.dimensionX());
        }
        if (data.dimensionY() != null) {
            product.setDimensionY(data.dimensionY());
        }
        if (data.dimensionZ() != null) {
            product.setDimensionZ(data.dimensionZ());
        }

        if (data.size() != null && !data.size().isBlank()) {
            product.setSize(data.size());
        }

        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        repository.delete(product);
    }
}
