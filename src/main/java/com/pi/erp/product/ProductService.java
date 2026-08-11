package com.pi.erp.product;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.product.category.Category;
import com.pi.erp.product.category.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    @Autowired
    private ProductRepository repository;

    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private CategoryRepository categoryRepository;

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
