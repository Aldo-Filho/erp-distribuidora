package com.pi.erp.supplier.product.option;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.catalog.ProductCatalogService;
import com.pi.erp.supplier.product.ProductSupplier;
import com.pi.erp.supplier.product.ProductSupplierRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSupplierOptionService {

    @Autowired
    private ProductSupplierOptionRepository repository;

    @Autowired
    private ProductSupplierRepository productSupplierRepository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private EntityManager entityManager;

    public Page<ProductSupplierOption> search(
        ProductSupplierOptionFilter filter,
        Pageable pageable
    ) {
        Specification<ProductSupplierOption> spec = Specification.allOf();
        if (filter.productSupplierId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("productSupplier").get("id"), filter.productSupplierId())
            );
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(
                    cb.lower(root.get("name")),
                    "%" + filter.query().toLowerCase(Locale.ROOT) + "%"
                )
            );
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), filter.active()));
        }
        return repository.findAll(spec, pageable);
    }

    @Transactional
    public ProductSupplierOption register(RequestProductSupplierOptionDTO data) {
        ProductSupplier link = findLink(data.productSupplierId());
        Product product = productCatalogService.lockProduct(link.getProduct().getId());
        productSupplierRepository
            .findByIdForUpdate(link.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
        entityManager.refresh(link);
        if (!link.getProduct().getId().equals(product.getId())) {
            throw new IllegalArgumentException("Supplier link changed. Retry the operation.");
        }
        validate(
            data.name(),
            data.baseFactor(),
            data.basePresentation(),
            data.cost(),
            data.avgDeliveryDays(),
            product
        );
        if (repository.existsByIdentity(link.getId(), data.name(), data.baseFactor(), null)) {
            throw new IllegalArgumentException(
                "This option name and factor already exist for this supplier link."
            );
        }
        ProductSupplierOption option = new ProductSupplierOption();
        option.setProductSupplier(link);
        option.setName(data.name().trim());
        option.setBaseFactor(data.baseFactor());
        option.setBasePresentation(data.basePresentation());
        option.setCost(data.cost());
        option.setAvgDeliveryDays(data.avgDeliveryDays());
        option.setSupplierProductCode(normalize(data.supplierProductCode()));
        option.setActive(data.active() == null || data.active());
        option.setConversionConfirmed(true);
        return repository.save(option);
    }

    @Transactional
    public ProductSupplierOption update(Long id, PatchProductSupplierOptionDTO data) {
        ProductSupplierOption option = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier option not found."));
        Product product = productCatalogService.lockProduct(
            option.getProductSupplier().getProduct().getId()
        );
        entityManager.refresh(option);
        productSupplierRepository
            .findByIdForUpdate(option.getProductSupplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
        entityManager.refresh(option.getProductSupplier());
        if (!option.getProductSupplier().getProduct().getId().equals(product.getId())) {
            throw new IllegalArgumentException("Supplier link changed. Retry the operation.");
        }
        if (Boolean.TRUE.equals(data.removeAvgDeliveryDays()) && data.avgDeliveryDays() != null) {
            throw new IllegalArgumentException(
                "Cannot provide and remove delivery days at the same time."
            );
        }
        String name = data.name() != null ? data.name() : option.getName();
        BigDecimal factor = data.baseFactor() != null ? data.baseFactor() : option.getBaseFactor();
        Boolean base =
            data.basePresentation() != null
                ? data.basePresentation()
                : option.getBasePresentation();
        BigDecimal cost = data.cost() != null ? data.cost() : option.getCost();
        Integer days = Boolean.TRUE.equals(data.removeAvgDeliveryDays())
            ? null
            : data.avgDeliveryDays() != null
              ? data.avgDeliveryDays()
              : option.getAvgDeliveryDays();
        boolean confirming =
            !option.isConversionConfirmed() && Boolean.TRUE.equals(data.confirmConversion());
        if (!option.isConversionConfirmed() && !confirming) {
            if (data.baseFactor() != null || data.basePresentation() != null) {
                throw new IllegalArgumentException(
                    "Confirm the legacy conversion explicitly with factor, type and cost."
                );
            }
            ProductCatalogService.validateMoney(cost);
            if (days != null && days < 0) {
                throw new IllegalArgumentException("Delivery days must not be negative.");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Option name is required.");
            }
        } else {
            if (
                confirming &&
                (data.baseFactor() == null ||
                    data.basePresentation() == null ||
                    data.cost() == null)
            ) {
                throw new IllegalArgumentException(
                    "Legacy confirmation requires explicit factor, type and cost."
                );
            }
            validate(name, factor, base, cost, days, product);
            if (
                repository.hasUsage(id) &&
                (factor.compareTo(option.getBaseFactor()) != 0 ||
                    !base.equals(option.getBasePresentation()))
            ) {
                throw new IllegalArgumentException(
                    "Cannot change a supplier option conversion already used."
                );
            }
        }
        if (repository.existsByIdentity(option.getProductSupplierId(), name, factor, id)) {
            throw new IllegalArgumentException(
                "This option name and factor already exist for this supplier link."
            );
        }
        option.setName(name.trim());
        option.setBaseFactor(factor);
        option.setBasePresentation(base);
        option.setCost(cost);
        option.setAvgDeliveryDays(days);
        if (data.supplierProductCode() != null) {
            option.setSupplierProductCode(normalize(data.supplierProductCode()));
        }
        if (data.active() != null) {
            option.setActive(data.active());
        }
        if (confirming) {
            option.setConversionConfirmed(true);
        }
        return repository.save(option);
    }

    public ProductSupplierOption findForPurchase(Long supplierId, Long productId, Long optionId) {
        ProductSupplier link = productSupplierRepository
            .findBySupplierIdAndProductId(supplierId, productId)
            .orElseThrow(() ->
                new IllegalArgumentException("Product is not linked to the supplier.")
            );
        productSupplierRepository
            .findByIdForUpdate(link.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
        entityManager.refresh(link);
        if (!link.isActive() || !link.getSupplier().isActive() || !link.getProduct().isActive()) {
            throw new IllegalArgumentException("Supplier, link and product must be active.");
        }
        ProductSupplierOption option;
        if (optionId == null) {
            List<ProductSupplierOption> options =
                repository.findByProductSupplierIdAndActiveTrueAndConversionConfirmedTrue(
                    link.getId()
                );
            if (options.size() != 1) {
                throw new IllegalArgumentException(
                    "Select one confirmed supplier option explicitly."
                );
            }
            option = options.getFirst();
        } else {
            option = repository
                .findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier option not found."));
        }
        entityManager.refresh(option);
        if (
            !option.getProductSupplierId().equals(link.getId()) ||
            !option.isActive() ||
            !option.isConversionConfirmed()
        ) {
            throw new IllegalArgumentException(
                "Select an active, confirmed option belonging to this supplier and product."
            );
        }
        validate(
            option.getName(),
            option.getBaseFactor(),
            option.getBasePresentation(),
            option.getCost(),
            option.getAvgDeliveryDays(),
            link.getProduct()
        );
        return option;
    }

    private ProductSupplier findLink(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product supplier ID is required.");
        }
        return productSupplierRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product supplier not found."));
    }

    private void validate(
        String name,
        BigDecimal factor,
        Boolean base,
        BigDecimal cost,
        Integer days,
        Product product
    ) {
        if (name == null || name.isBlank() || base == null) {
            throw new IllegalArgumentException("Option name and presentation type are required.");
        }
        ProductQuantity.requireBase(factor, product.getUnit(), false);
        if (base && factor.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Base option factor must be one.");
        }
        ProductCatalogService.validateMoney(cost);
        if (days != null && days < 0) {
            throw new IllegalArgumentException("Delivery days must not be negative.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
