package com.pi.erp.product.presentation;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.catalog.ProductCatalogService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductPresentationService {

    @Autowired
    private ProductPresentationRepository repository;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private EntityManager entityManager;

    public Page<ProductPresentation> search(ProductPresentationFilter filter, Pageable pageable) {
        Specification<ProductPresentation> spec = Specification.allOf();
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
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
            spec = spec.and((root, query, cb) ->
                cb.equal(
                    cb.or(
                        cb.and(
                            cb.isTrue(root.get("basePresentation")),
                            cb.isTrue(root.get("product").get("active")),
                            cb.isTrue(root.get("product").get("catalog").get("active"))
                        ),
                        cb.and(
                            cb.isFalse(root.get("basePresentation")),
                            cb.isTrue(root.get("active"))
                        )
                    ),
                    filter.active()
                )
            );
        }
        return repository.findAll(spec, pageable);
    }

    @Transactional
    public ProductPresentation register(RequestProductPresentationDTO data) {
        Product product = productCatalogService.lockProduct(data.productId());
        validate(data.name(), data.baseFactor(), data.price(), product);
        if (repository.existsByIdentity(product.getId(), data.name(), data.baseFactor(), null)) {
            throw new IllegalArgumentException(
                "This presentation name and factor already exist for this product."
            );
        }
        ProductPresentation presentation = new ProductPresentation();
        presentation.setProduct(product);
        presentation.setName(data.name().trim());
        presentation.setBaseFactor(data.baseFactor());
        presentation.setPrice(data.price());
        presentation.setActive(data.active() == null || data.active());
        return repository.save(presentation);
    }

    @Transactional
    public ProductPresentation update(Long id, PatchProductPresentationDTO data) {
        ProductPresentation presentation = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product presentation not found."));
        Product product = productCatalogService.lockProduct(presentation.getProductId());
        entityManager.refresh(presentation);
        if (presentation.isBasePresentation()) {
            throw new IllegalArgumentException("Base presentation is controlled by the product.");
        }
        if (Boolean.TRUE.equals(data.removePrice()) && data.price() != null) {
            throw new IllegalArgumentException(
                "Cannot provide and remove presentation price at the same time."
            );
        }
        String name = data.name() != null ? data.name() : presentation.getName();
        BigDecimal factor =
            data.baseFactor() != null ? data.baseFactor() : presentation.getBaseFactor();
        BigDecimal price = Boolean.TRUE.equals(data.removePrice())
            ? null
            : data.price() != null
              ? data.price()
              : presentation.getPrice();
        validate(name, factor, price, product);
        if (factor.compareTo(presentation.getBaseFactor()) != 0 && repository.hasUsage(id)) {
            throw new IllegalArgumentException(
                "Cannot change a presentation factor already used. Create another presentation."
            );
        }
        if (repository.existsByIdentity(product.getId(), name, factor, id)) {
            throw new IllegalArgumentException(
                "This presentation name and factor already exist for this product."
            );
        }
        presentation.setName(name.trim());
        presentation.setBaseFactor(factor);
        presentation.setPrice(price);
        if (data.active() != null) {
            presentation.setActive(data.active());
        }
        return repository.save(presentation);
    }

    public ProductPresentation findForProduct(Long productId, Long presentationId) {
        ProductPresentation presentation =
            presentationId == null
                ? repository
                      .findByProductIdAndBasePresentationTrue(productId)
                      .orElseThrow(() ->
                          new IllegalArgumentException("Confirm the product base unit first.")
                      )
                : repository
                      .findById(presentationId)
                      .orElseThrow(() ->
                          new ResourceNotFoundException("Product presentation not found.")
                      );
        entityManager.refresh(presentation);
        if (!presentation.getProductId().equals(productId)) {
            throw new IllegalArgumentException(
                "Presentation does not belong to the selected product."
            );
        }
        if (!presentation.isActive()) {
            throw new IllegalArgumentException("Presentation must be active.");
        }
        return presentation;
    }

    private void validate(String name, BigDecimal factor, BigDecimal price, Product product) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Presentation name is required.");
        }
        ProductQuantity.requireBase(factor, product.getUnit(), false);
        if (price != null) {
            ProductCatalogService.validateMoney(price);
        }
    }
}
