package com.pi.erp.product;

import com.pi.erp.product.catalog.PatchProductCatalogDTO;
import com.pi.erp.product.catalog.ProductCatalogRepository;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.product.catalog.ProductCatalogType;
import com.pi.erp.product.catalog.RequestCatalogItemDTO;
import com.pi.erp.product.catalog.RequestProductCatalogDTO;
import com.pi.erp.product.presentation.ProductPresentationRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private ProductCatalogRepository productCatalogRepository;

    @Autowired
    private ProductPresentationRepository productPresentationRepository;

    public List<Product> search(ProductFilter filter) {
        Specification<Product> spec = Specification.allOf();

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("catalog").get("name")),
                    "%" + filter.name().toLowerCase() + "%"
                )
            );
        }
        if (filter.sku() != null && !filter.sku().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sku"), filter.sku()));
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("catalog").get("category").get("id"), filter.categoryId())
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("catalog").get("brand").get("id"), filter.brandId())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(
                    cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("catalog").get("active"))
                    ),
                    filter.active()
                )
            );
        }

        if (filter.catalogId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("catalog").get("id"), filter.catalogId())
            );
        }
        return repository.findAll(spec);
    }

    @Transactional
    public Product register(RequestProductDTO data) {
        var item = new RequestCatalogItemDTO(
            data.cost(),
            data.price(),
            data.weightKg(),
            data.color(),
            data.dimensionX(),
            data.dimensionY(),
            data.dimensionZ(),
            data.size(),
            data.catalogId() == null ? null : data.active(),
            data.attributes(),
            data.stock()
        );
        if (data.catalogId() != null) {
            if (
                data.name() != null ||
                data.brandId() != null ||
                data.categoryId() != null ||
                data.unit() != null
            ) {
                throw new IllegalArgumentException(
                    "Use the catalog endpoint to edit shared variant data."
                );
            }
            return productCatalogService.registerItem(data.catalogId(), item);
        }
        var catalog = productCatalogService.register(
            new RequestProductCatalogDTO(
                data.name(),
                data.brandId(),
                data.categoryId(),
                data.unit(),
                ProductCatalogType.SIMPLE,
                Set.of(),
                data.active(),
                List.of(item)
            )
        );
        return repository.findByCatalogIdOrderByIdAsc(catalog.getId()).getFirst();
    }

    @Transactional
    public Product update(Long id, PatchProductDTO data) {
        Product product = productCatalogService.lockProduct(id);
        if (data.catalogId() != null && !data.catalogId().equals(product.getCatalogId())) {
            throw new IllegalArgumentException("Cannot move an item to another catalog.");
        }
        boolean simple = product.getCatalog().getType() == ProductCatalogType.SIMPLE;
        boolean shared =
            data.name() != null ||
            data.brandId() != null ||
            data.categoryId() != null ||
            data.unit() != null ||
            Boolean.TRUE.equals(data.removeCategory());
        if (!simple && shared) {
            throw new IllegalArgumentException(
                "Use the catalog endpoint to edit shared variant data."
            );
        }
        if (simple && (shared || data.active() != null)) {
            productCatalogService.update(
                product.getCatalogId(),
                new PatchProductCatalogDTO(
                    data.name(),
                    data.brandId(),
                    data.categoryId(),
                    data.unit(),
                    null,
                    data.active(),
                    data.removeCategory()
                )
            );
        }
        if (!simple && (data.color() != null || data.size() != null)) {
            throw new IllegalArgumentException("Use variant attributes for color and size.");
        }
        if (data.attributes() != null) {
            String key = productCatalogService.variantKey(product.getCatalog(), data.attributes());
            if (
                repository.existsByCatalogIdAndVariantKeyAndIdNot(product.getCatalogId(), key, id)
            ) {
                throw new IllegalArgumentException(
                    "This variant combination already exists in the catalog."
                );
            }
            product.setVariantKey(key);
            product.setAttributes(new HashMap<>(data.attributes()));
        }
        if (data.cost() != null) {
            ProductCatalogService.validateMoney(data.cost());
            product.setCost(data.cost());
        }
        if (data.price() != null) {
            ProductCatalogService.validateMoney(data.price());
            product.setPrice(data.price());
        }
        product.setWeightKg(
            optionalNumber(product.getWeightKg(), data.weightKg(), data.removeWeightKg())
        );
        product.setDimensionX(
            optionalNumber(product.getDimensionX(), data.dimensionX(), data.removeDimensionX())
        );
        product.setDimensionY(
            optionalNumber(product.getDimensionY(), data.dimensionY(), data.removeDimensionY())
        );
        product.setDimensionZ(
            optionalNumber(product.getDimensionZ(), data.dimensionZ(), data.removeDimensionZ())
        );
        if (data.color() != null) {
            product.setColor(data.color().isBlank() ? null : data.color());
        }
        if (data.size() != null) {
            product.setSize(data.size().isBlank() ? null : data.size());
        }
        if (!simple && data.active() != null) {
            if (data.active() && !product.getCatalog().isActive()) {
                throw new IllegalArgumentException(
                    "Activate the catalog before activating this variant."
                );
            }
            product.setActive(data.active());
        }
        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productCatalogService.lockProduct(id);
        var catalog = product.getCatalog();
        if (
            catalog.getType() == ProductCatalogType.VARIANT &&
            repository.countByCatalogId(catalog.getId()) <= 1
        ) {
            throw new IllegalArgumentException("Cannot delete the last variant of a catalog.");
        }
        // FK constraints deliberately protect stock, prices, supplier links and commercial history.
        // Unused base presentations are server-owned and can be removed with the unused SKU.
        productPresentationRepository.deleteUnusedByProductId(id);
        repository.delete(product);
        repository.flush();
        if (catalog.getType() == ProductCatalogType.SIMPLE) {
            productCatalogRepository.delete(catalog);
        }
    }

    private BigDecimal optionalNumber(BigDecimal current, BigDecimal value, Boolean remove) {
        if (Boolean.TRUE.equals(remove) && value != null) {
            throw new IllegalArgumentException(
                "Cannot provide and remove an optional number at the same time."
            );
        }
        return Boolean.TRUE.equals(remove) ? null : value != null ? value : current;
    }
}
