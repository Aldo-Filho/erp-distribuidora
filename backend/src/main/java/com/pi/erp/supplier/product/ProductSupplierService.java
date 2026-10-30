package com.pi.erp.supplier.product;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.SupplierRepository;
import com.pi.erp.supplier.product.option.ProductSupplierOption;
import com.pi.erp.supplier.product.option.ProductSupplierOptionRepository;
import com.pi.erp.supplier.product.option.ProductSupplierOptionService;
import com.pi.erp.supplier.product.option.RequestProductSupplierOptionDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSupplierService {

    @Autowired
    private ProductSupplierRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private ProductSupplierOptionService productSupplierOptionService;

    @Autowired
    private ProductSupplierOptionRepository productSupplierOptionRepository;

    @Autowired
    private EntityManager entityManager;

    public List<ProductSupplier> search(ProductSupplierFilter filter) {
        if (
            filter.minCost() != null &&
            filter.maxCost() != null &&
            filter.minCost().compareTo(filter.maxCost()) > 0
        ) {
            throw new IllegalArgumentException("Minimum cost cannot be greater than maximum cost.");
        }

        Specification<ProductSupplier> spec = Specification.allOf();

        if (filter.supplierId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), filter.supplierId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
            );
        }
        if (filter.brandId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(
                    root.get("product").get("catalog").get("brand").get("id"),
                    filter.brandId()
                )
            );
        }
        if (filter.categoryId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(
                    root.get("product").get("catalog").get("category").get("id"),
                    filter.categoryId()
                )
            );
        }
        if (filter.productName() != null && !filter.productName().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("product").get("catalog").get("name")),
                    "%" + filter.productName().toLowerCase(Locale.ROOT) + "%"
                )
            );
        }
        if (
            (filter.supplierProductCode() != null && !filter.supplierProductCode().isBlank()) ||
            filter.minCost() != null ||
            filter.maxCost() != null ||
            filter.avgDeliveryDays() != null
        ) {
            spec = spec.and((root, query, cb) -> {
                var sub = query.subquery(Long.class);
                var option = sub.from(ProductSupplierOption.class);
                List<Predicate> conditions = new ArrayList<>();
                conditions.add(cb.equal(option.get("productSupplier"), root));
                if (
                    filter.supplierProductCode() != null && !filter.supplierProductCode().isBlank()
                ) {
                    conditions.add(
                        cb.like(
                            cb.lower(option.get("supplierProductCode")),
                            "%" + filter.supplierProductCode().toLowerCase(Locale.ROOT) + "%"
                        )
                    );
                }
                if (filter.minCost() != null) {
                    conditions.add(cb.greaterThanOrEqualTo(option.get("cost"), filter.minCost()));
                }
                if (filter.maxCost() != null) {
                    conditions.add(cb.lessThanOrEqualTo(option.get("cost"), filter.maxCost()));
                }
                if (filter.avgDeliveryDays() != null) {
                    conditions.add(
                        cb.equal(option.get("avgDeliveryDays"), filter.avgDeliveryDays())
                    );
                }
                sub.select(option.get("id")).where(conditions.toArray(Predicate[]::new));
                return cb.exists(sub);
            });
        }
        if (filter.preferred() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("preferred"), filter.preferred())
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }

        return repository.findAll(spec);
    }

    @Transactional
    public ProductSupplier register(RequestProductSupplierDTO data) {
        Supplier supplier = findSupplier(data.supplierId());
        Product product = productCatalogService.lockProduct(data.productId());

        if (repository.existsByProductIdAndSupplierId(data.productId(), data.supplierId())) {
            throw new IllegalArgumentException("This product is already linked to this supplier.");
        }

        ProductSupplier productSupplier = new ProductSupplier(data, product, supplier);
        if (productSupplier.isPreferred()) {
            clearPreference(product.getId(), null);
        }
        ProductSupplier savedProductSupplier = repository.save(productSupplier);
        if (data.option() != null) {
            if (
                data.cost() != null ||
                data.avgDeliveryDays() != null ||
                data.supplierProductCode() != null
            ) {
                throw new IllegalArgumentException(
                    "Use either the first option or legacy commercial fields."
                );
            }
            var option = data.option();
            productSupplierOptionService.register(
                new RequestProductSupplierOptionDTO(
                    savedProductSupplier.getId(),
                    option.name(),
                    option.baseFactor(),
                    option.basePresentation(),
                    option.cost(),
                    option.avgDeliveryDays(),
                    option.supplierProductCode(),
                    option.active(),
                    option.confirmConversion()
                )
            );
        } else if (
            data.cost() != null ||
            data.avgDeliveryDays() != null ||
            data.supplierProductCode() != null
        ) {
            ProductCatalogService.validateMoney(data.cost());
            var option = new ProductSupplierOption();
            option.setProductSupplier(savedProductSupplier);
            option.setName("Legacy conversion pending");
            option.setCost(data.cost());
            option.setAvgDeliveryDays(data.avgDeliveryDays());
            option.setSupplierProductCode(data.supplierProductCode());
            option.setActive(savedProductSupplier.isActive());
            savedProductSupplier.setLegacyOption(productSupplierOptionRepository.save(option));
        }
        return repository.save(savedProductSupplier);
    }

    @Transactional
    public ProductSupplier update(Long id, PatchProductSupplierDTO data) {
        ProductSupplier productSupplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));

        Long originalProductId = productSupplier.getProduct().getId();
        Product targetProduct =
            data.productId() != null ? findProduct(data.productId()) : productSupplier.getProduct();
        productCatalogService.lockProducts(List.of(productSupplier.getProduct(), targetProduct));
        repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
        entityManager.refresh(productSupplier);
        if (!productSupplier.getProduct().getId().equals(originalProductId)) {
            throw new IllegalArgumentException("Supplier link changed. Retry the operation.");
        }
        Supplier supplier =
            data.supplierId() != null
                ? findSupplier(data.supplierId())
                : productSupplier.getSupplier();
        boolean moving =
            !targetProduct.getId().equals(originalProductId) ||
            !supplier.getId().equals(productSupplier.getSupplier().getId());
        if (moving) {
            if (
                repository.existsByProductIdAndSupplierIdAndIdNot(
                    targetProduct.getId(),
                    supplier.getId(),
                    id
                )
            ) {
                throw new IllegalArgumentException(
                    "This product is already linked to this supplier."
                );
            }
            for (var option : productSupplierOptionRepository.findByProductSupplierIdOrderByIdAsc(
                id
            )) {
                if (productSupplierOptionRepository.hasUsage(option.getId())) {
                    throw new IllegalArgumentException(
                        "Cannot move a supplier link with contracted options. Create another link."
                    );
                }
                if (option.isConversionConfirmed()) {
                    ProductQuantity.requireBase(
                        option.getBaseFactor(),
                        targetProduct.getUnit(),
                        false
                    );
                }
            }
            productSupplier.setProduct(targetProduct);
            productSupplier.setSupplier(supplier);
        }
        if (
            data.cost() != null ||
            data.avgDeliveryDays() != null ||
            data.supplierProductCode() != null
        ) {
            if (productSupplier.getLegacyOption() == null) {
                throw new IllegalArgumentException(
                    "Provide an optionId through the supplier option endpoint."
                );
            }
            var option = productSupplier.getLegacyOption();
            if (data.cost() != null) {
                ProductCatalogService.validateMoney(data.cost());
                option.setCost(data.cost());
            }
            if (data.avgDeliveryDays() != null) {
                option.setAvgDeliveryDays(data.avgDeliveryDays());
            }
            if (data.supplierProductCode() != null) {
                option.setSupplierProductCode(
                    data.supplierProductCode().isBlank() ? null : data.supplierProductCode()
                );
            }
            productSupplierOptionRepository.save(option);
        }
        boolean preferred =
            data.preferred() != null ? data.preferred() : productSupplier.isPreferred();
        if (preferred) {
            // Flush the old identity without a preference before selecting the new preferred link.
            productSupplier.setPreferred(false);
            repository.saveAndFlush(productSupplier);
            clearPreference(productSupplier.getProduct().getId(), id);
        }
        productSupplier.setPreferred(preferred);
        if (data.active() != null) {
            productSupplier.setActive(data.active());
        }

        return repository.save(productSupplier);
    }

    @Transactional
    public void delete(Long id) {
        ProductSupplier productSupplier = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));

        Long originalProductId = productSupplier.getProduct().getId();
        productCatalogService.lockProduct(originalProductId);
        repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
        entityManager.refresh(productSupplier);
        if (!productSupplier.getProduct().getId().equals(originalProductId)) {
            throw new IllegalArgumentException("Supplier link changed. Retry the operation.");
        }
        var options = productSupplierOptionRepository.findByProductSupplierIdOrderByIdAsc(id);
        for (var option : options) {
            if (productSupplierOptionRepository.hasUsage(option.getId())) {
                throw new IllegalArgumentException(
                    "Cannot delete a supplier link with contracted options."
                );
            }
        }
        productSupplier.setLegacyOption(null);
        repository.saveAndFlush(productSupplier);
        productSupplierOptionRepository.deleteAll(options);
        productSupplierOptionRepository.flush();
        repository.delete(productSupplier);
    }

    private void clearPreference(Long productId, Long selectedId) {
        for (ProductSupplier link : repository.findByProductIdForUpdate(productId)) {
            if (!link.getId().equals(selectedId) && link.isPreferred()) {
                link.setPreferred(false);
                repository.save(link);
            }
        }
        repository.flush();
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required.");
        }

        return supplierRepository
            .findById(supplierId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found."));
    }
}
