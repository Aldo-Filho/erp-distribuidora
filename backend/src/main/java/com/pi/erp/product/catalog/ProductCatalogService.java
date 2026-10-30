package com.pi.erp.product.catalog;

import com.pi.erp.auth.CurrentUser;
import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.product.ProductUnit;
import com.pi.erp.product.SkuGenerator;
import com.pi.erp.product.brand.BrandRepository;
import com.pi.erp.product.category.CategoryRepository;
import com.pi.erp.product.presentation.ProductPresentation;
import com.pi.erp.product.presentation.ProductPresentationRepository;
import com.pi.erp.stock.RequestStockItemDTO;
import com.pi.erp.stock.StockItemRepository;
import com.pi.erp.stock.StockItemService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductCatalogService {

    // Canonical keys must not depend on response formatting configured on the application mapper.
    private static final ObjectMapper VARIANT_KEY_MAPPER = new ObjectMapper();

    @Autowired
    private ProductCatalogRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductPresentationRepository productPresentationRepository;

    @Autowired
    private StockItemService stockItemService;

    @Autowired
    private StockItemRepository stockItemRepository;

    @Autowired
    private SkuGenerator skuGenerator;

    @Autowired
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<ProductCatalog> search(ProductCatalogFilter filter, Pageable pageable) {
        Specification<ProductCatalog> spec = Specification.allOf();
        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("name")), contains(filter.name()))
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("brand").get("id"), filter.brandId())
            );
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("category").get("id"), filter.categoryId())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        if (filter.sku() != null && !filter.sku().isBlank()) {
            spec = spec.and((root, query, cb) -> {
                var sub = query.subquery(Long.class);
                var item = sub.from(Product.class);
                sub.select(item.get("id")).where(
                    cb.equal(item.get("catalog"), root),
                    cb.equal(item.get("sku"), filter.sku())
                );
                return cb.exists(sub);
            });
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            spec = spec.and((root, query, cb) -> {
                var sub = query.subquery(Long.class);
                var item = sub.from(Product.class);
                MapJoin<Product, ProductCharacteristic, String> attribute = item.joinMap(
                    "attributes",
                    JoinType.LEFT
                );
                sub.select(item.get("id")).where(
                    cb.equal(item.get("catalog"), root),
                    cb.or(
                        cb.like(cb.lower(item.get("sku")), contains(filter.query())),
                        cb.like(cb.lower(attribute.value()), contains(filter.query()))
                    )
                );
                return cb.or(
                    cb.like(cb.lower(root.get("name")), contains(filter.query())),
                    cb.exists(sub)
                );
            });
        }
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchItems(Long id, String query, Boolean active, Pageable pageable) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product catalog not found.");
        }
        Specification<Product> spec = Specification.allOf();
        spec = spec.and((root, criteria, cb) -> cb.equal(root.get("catalog").get("id"), id));
        if (active != null) {
            spec = spec.and((root, criteria, cb) ->
                cb.equal(
                    cb.and(
                        cb.isTrue(root.get("active")),
                        cb.isTrue(root.get("catalog").get("active"))
                    ),
                    active
                )
            );
        }
        if (query != null && !query.isBlank()) {
            spec = spec.and((root, criteria, cb) -> {
                var sub = criteria.subquery(Long.class);
                var item = sub.from(Product.class);
                MapJoin<Product, ProductCharacteristic, String> value = item.joinMap("attributes");
                String term = contains(query);
                sub.select(item.get("id")).where(
                    cb.equal(item, root),
                    cb.like(cb.lower(value.value()), term)
                );
                return cb.or(
                    cb.like(cb.lower(root.get("catalog").get("name")), term),
                    cb.like(cb.lower(root.get("sku")), term),
                    cb.exists(sub)
                );
            });
        }
        return productRepository.findAll(spec, pageable);
    }

    @Transactional
    public ProductCatalog register(RequestProductCatalogDTO data) {
        if (
            data.name() == null ||
            data.name().isBlank() ||
            data.unit() == null ||
            data.type() == null
        ) {
            throw new IllegalArgumentException("Catalog name, unit and type are required.");
        }
        if (
            data.items() == null ||
            data.items().isEmpty() ||
            (data.type() == ProductCatalogType.SIMPLE && data.items().size() != 1)
        ) {
            throw new IllegalArgumentException(
                "Simple catalogs require one item and variant catalogs at least one item."
            );
        }
        ProductCatalog catalog = new ProductCatalog();
        catalog.setName(data.name().trim());
        catalog.setBrand(
            brandRepository
                .findById(requireId(data.brandId(), "Brand"))
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found."))
        );
        if (data.categoryId() != null) {
            catalog.setCategory(
                categoryRepository
                    .findById(data.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found."))
            );
        }
        catalog.setUnit(data.unit());
        catalog.setType(data.type());
        catalog.setCharacteristics(
            data.characteristics() == null ? new HashSet<>() : new HashSet<>(data.characteristics())
        );
        catalog.setActive(data.active() == null || data.active());
        if (
            (catalog.getType() == ProductCatalogType.SIMPLE &&
                !catalog.getCharacteristics().isEmpty()) ||
            (catalog.getType() == ProductCatalogType.VARIANT &&
                catalog.getCharacteristics().isEmpty()) ||
            catalog.getCharacteristics().contains(null)
        ) {
            throw new IllegalArgumentException("Characteristics must match the catalog type.");
        }
        Set<String> keys = new HashSet<>();
        for (RequestCatalogItemDTO item : data.items()) {
            validateItem(catalog, item);
            if (!keys.add(variantKey(catalog, item.attributes()))) {
                throw new IllegalArgumentException("Duplicate variant combination.");
            }
        }
        ProductCatalog savedCatalog = repository.save(catalog);
        for (RequestCatalogItemDTO item : data.items()) {
            saveItem(savedCatalog, item);
        }
        return savedCatalog;
    }

    @Transactional
    public ProductCatalog update(Long id, PatchProductCatalogDTO data) {
        ProductCatalog catalog = lockCatalog(id);
        List<Product> items = productRepository.findByCatalogIdOrderByIdAsc(id);
        for (Product item : items) {
            productRepository.findByIdForUpdate(item.getId());
        }
        if (Boolean.TRUE.equals(data.removeCategory()) && data.categoryId() != null) {
            throw new IllegalArgumentException(
                "Cannot provide and remove a category at the same time."
            );
        }
        if (data.name() != null && data.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (
            data.characteristics() != null &&
            !data.characteristics().equals(catalog.getCharacteristics())
        ) {
            throw new IllegalArgumentException(
                "Catalog characteristics cannot change after item creation."
            );
        }
        if (data.unit() != null && data.unit() != catalog.getUnit()) {
            if (catalog.getUnit() != null && repository.hasUnitUsage(id)) {
                throw new IllegalArgumentException(
                    "Cannot change a base unit already used by stock or operations."
                );
            }
            // Defining a legacy unit labels existing numbers; it never converts them or rewrites snapshots.
            for (Product item : items) {
                validateExistingStock(item.getId(), data.unit());
            }
            catalog.setUnit(data.unit());
            for (Product item : items) {
                ensureBasePresentation(item);
            }
        }
        if (data.name() != null) {
            catalog.setName(data.name().trim());
        }
        if (data.brandId() != null) {
            catalog.setBrand(
                brandRepository
                    .findById(data.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found."))
            );
        }
        if (Boolean.TRUE.equals(data.removeCategory())) {
            catalog.setCategory(null);
        } else if (data.categoryId() != null) {
            catalog.setCategory(
                categoryRepository
                    .findById(data.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found."))
            );
        }
        if (data.active() != null) {
            catalog.setActive(data.active());
        }
        return repository.save(catalog);
    }

    @Transactional
    public Product registerItem(Long id, RequestCatalogItemDTO data) {
        ProductCatalog catalog = lockCatalog(id);
        if (catalog.getType() != ProductCatalogType.VARIANT) {
            throw new IllegalArgumentException("Only variant catalogs accept additional items.");
        }
        validateItem(catalog, data);
        String key = variantKey(catalog, data.attributes());
        if (productRepository.existsByCatalogIdAndVariantKey(id, key)) {
            throw new IllegalArgumentException(
                "This variant combination already exists in the catalog."
            );
        }
        return saveItem(catalog, data);
    }

    public ProductCatalog lockCatalog(Long id) {
        ProductCatalog catalog = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product catalog not found."));
        entityManager.refresh(catalog);
        return catalog;
    }

    public Product lockProduct(Long id) {
        requireId(id, "Product");
        Long catalogId = productRepository
            .findCatalogId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        lockCatalog(catalogId);
        Product product = productRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        entityManager.refresh(product);
        return product;
    }

    public void lockProducts(List<Product> products) {
        products.stream().map(Product::getCatalogId).distinct().sorted().forEach(this::lockCatalog);
        products
            .stream()
            .map(Product::getId)
            .distinct()
            .sorted()
            .forEach(id -> {
                Product product = productRepository
                    .findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
                entityManager.refresh(product);
            });
    }

    public String variantKey(
        ProductCatalog catalog,
        Map<ProductCharacteristic, String> attributes
    ) {
        Map<ProductCharacteristic, String> values = attributes == null ? Map.of() : attributes;
        if (!values.keySet().equals(catalog.getCharacteristics())) {
            throw new IllegalArgumentException(
                "Variant values must match all catalog characteristics."
            );
        }
        if (catalog.getType() == ProductCatalogType.SIMPLE) {
            return "";
        }
        Map<String, String> normalized = new TreeMap<>();
        values.forEach((key, value) -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Variant values must not be blank.");
            }
            normalized.put(key.name(), value.trim().toLowerCase(Locale.ROOT));
        });
        try {
            return VARIANT_KEY_MAPPER.writeValueAsString(normalized);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid variant values.", exception);
        }
    }

    public void ensureBasePresentation(Product product) {
        if (product.getUnit() == null) {
            return;
        }
        ProductPresentation base = productPresentationRepository
            .findByProductIdAndBasePresentationTrue(product.getId())
            .orElseGet(ProductPresentation::new);
        base.setProduct(product);
        base.setName(product.getUnit().name());
        base.setBaseFactor(BigDecimal.ONE);
        base.setBasePresentation(true);
        base.setActive(true);
        base.setPrice(null);
        productPresentationRepository.save(base);
    }

    public static void validateMoney(BigDecimal value) {
        if (
            value == null ||
            value.signum() < 0 ||
            value.compareTo(new BigDecimal("9999999999.99")) > 0 ||
            value.stripTrailingZeros().scale() > 2
        ) {
            throw new IllegalArgumentException(
                "Money must be non-negative, at most 9999999999.99 and have at most two decimal places."
            );
        }
    }

    private Product saveItem(ProductCatalog catalog, RequestCatalogItemDTO data) {
        Product product = new Product();
        product.setCatalog(catalog);
        product.setSku(skuGenerator.generate(productRepository.nextSkuNumber()));
        product.setCost(data.cost());
        product.setPrice(data.price());
        product.setWeightKg(data.weightKg());
        product.setColor(normalize(data.color()));
        product.setSize(normalize(data.size()));
        product.setDimensionX(data.dimensionX());
        product.setDimensionY(data.dimensionY());
        product.setDimensionZ(data.dimensionZ());
        product.setActive(
            catalog.getType() == ProductCatalogType.SIMPLE || data.active() == null || data.active()
        );
        product.setAttributes(
            data.attributes() == null ? new TreeMap<>() : new TreeMap<>(data.attributes())
        );
        product.setVariantKey(variantKey(catalog, data.attributes()));
        Product savedProduct = productRepository.save(product);
        ensureBasePresentation(savedProduct);
        if (data.stock() != null) {
            var stock = data.stock();
            stockItemService.register(
                new RequestStockItemDTO(
                    stock.warehouseId(),
                    savedProduct.getId(),
                    stock.quantity(),
                    stock.reservedQuantity(),
                    stock.minQuantity(),
                    stock.maxQuantity()
                )
            );
        }
        return savedProduct;
    }

    private void validateItem(ProductCatalog catalog, RequestCatalogItemDTO data) {
        if (data == null) {
            throw new IllegalArgumentException("Catalog item is required.");
        }
        validateMoney(data.cost());
        validateMoney(data.price());
        if (catalog.getType() == ProductCatalogType.SIMPLE && Boolean.FALSE.equals(data.active())) {
            throw new IllegalArgumentException("Set simple product availability on its catalog.");
        }
        if (
            catalog.getType() == ProductCatalogType.VARIANT &&
            (data.color() != null || data.size() != null)
        ) {
            throw new IllegalArgumentException("Use variant attributes for color and size.");
        }
        variantKey(catalog, data.attributes());
        if (data.stock() != null) {
            CurrentUser.require("stock.configure");
            ProductQuantity.requireBase(data.stock().quantity(), catalog.getUnit(), true);
            ProductQuantity.requireBase(data.stock().reservedQuantity(), catalog.getUnit(), true);
            ProductQuantity.requireBase(data.stock().minQuantity(), catalog.getUnit(), true);
            if (data.stock().maxQuantity() != null) {
                ProductQuantity.requireBase(data.stock().maxQuantity(), catalog.getUnit(), true);
            }
        }
    }

    private void validateExistingStock(Long productId, ProductUnit unit) {
        for (var stock : stockItemRepository.findByProductIdOrderByIdAsc(productId)) {
            ProductQuantity.requireBase(stock.getQuantity(), unit, true);
            ProductQuantity.requireBase(stock.getReservedQuantity(), unit, true);
            ProductQuantity.requireBase(stock.getMinQuantity(), unit, true);
            if (stock.getMaxQuantity() != null) {
                ProductQuantity.requireBase(stock.getMaxQuantity(), unit, true);
            }
        }
    }

    private Long requireId(Long id, String field) {
        if (id == null) {
            throw new IllegalArgumentException(field + " ID is required.");
        }
        return id;
    }

    private String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
