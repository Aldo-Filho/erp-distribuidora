package com.pi.erp.product;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.product.category.Category;
import com.pi.erp.product.category.CategoryRepository;
import com.pi.erp.stock.RequestStockItemDTO;
import com.pi.erp.stock.StockItemService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockItemService stockItemService;

    public List<Product> search(ProductFilter filter) {
        Specification<Product> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%")
            );
        }
        if (filter.sku() != null && !filter.sku().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("sku"), filter.sku())
            );
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("category").get("id"), filter.categoryId())
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("brand").get("id"), filter.brandId())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("active"), filter.active())
            );
        }

        return repository.findAll(spec);
    }

    @Transactional
    public Product register(RequestProductDTO data) {
        if (repository.existsBySku(data.sku())) {
            throw new IllegalArgumentException("Product already exists.");
        }

        Brand brand = findBrand(data.brandId());
        Category category = findCategory(data.categoryId());

        Product product = new Product(data, brand, category);
        Product savedProduct = repository.save(product);

        if (data.stock() != null) {
            RequestProductDTO.InitialStockDTO stock = data.stock();
            RequestStockItemDTO stockData = new RequestStockItemDTO(
                stock.warehouseId(),
                savedProduct.getId(),
                stock.quantity(),
                stock.reservedQuantity(),
                stock.minQuantity(),
                stock.maxQuantity()
            );
            stockItemService.register(stockData);
        }

        return savedProduct;
    }

    public Product update(Long id, PatchProductDTO data) {
        Product product = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (Boolean.TRUE.equals(data.removeCategory()) && data.categoryId() != null) {
            throw new IllegalArgumentException(
                "Cannot provide a category and request its removal at the same time."
            );
        }

        if (data.name() != null && data.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }

        Brand brand = product.getBrand();
        Category category = product.getCategory();

        if (data.brandId() != null) {
            brand = findBrand(data.brandId());
        }
        if (Boolean.TRUE.equals(data.removeCategory())) {
            category = null;
        } else if (data.categoryId() != null) {
            category = findCategory(data.categoryId());
        }

        product.setBrand(brand);
        product.setCategory(category);

        if (data.name() != null) {
            product.setName(data.name());
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
        if (data.color() != null) {
            product.setColor(data.color().isBlank() ? null : data.color());
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
        if (data.size() != null) {
            product.setSize(data.size().isBlank() ? null : data.size());
        }
        if (data.active() != null) {
            product.setActive(data.active());
        }

        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        repository.delete(product);
    }

    private Brand findBrand(Long brandId) {
        if (brandId == null) {
            throw new IllegalArgumentException("Brand ID is required.");
        }

        return brandRepository
            .findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found."));
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    }
}
