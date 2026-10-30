package com.pi.erp.price.item;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.price.PriceTable;
import com.pi.erp.price.PriceTableRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.product.presentation.ProductPresentation;
import com.pi.erp.product.presentation.ProductPresentationRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceTableItemService {

    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    @Autowired
    private PriceTableItemRepository repository;

    @Autowired
    private PriceTableRepository priceTableRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductPresentationRepository productPresentationRepository;

    public List<PriceTableItem> search(PriceTableItemFilter filter) {
        Specification<PriceTableItem> spec = Specification.allOf();

        if (filter.priceTableId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("priceTable").get("id"), filter.priceTableId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
            );
        }

        if (filter.presentationId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("presentation").get("id"), filter.presentationId())
            );
        }
        return repository.findAll(spec);
    }

    @Transactional
    public PriceTableItem register(RequestPriceTableItemDTO data) {
        validatePrice(data.price());
        PriceTable priceTable = findPriceTable(data.priceTableId());
        Product product = findProduct(data.productId());

        var presentation = findPresentation(product, data.presentationId());
        if (
            repository.existsByIdentity(
                priceTable.getId(),
                product.getId(),
                presentation == null ? null : presentation.getId(),
                null
            )
        ) {
            throw new IllegalArgumentException("This product already exists in this price table.");
        }

        PriceTableItem priceTableItem = new PriceTableItem(data, priceTable, product);
        priceTableItem.setPresentation(presentation);
        return repository.save(priceTableItem);
    }

    @Transactional
    public PriceTableItem update(Long id, PatchPriceTableItemDTO data) {
        PriceTableItem priceTableItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Price table item not found."));

        entityManager.refresh(priceTableItem);

        BigDecimal price = data.price() != null ? data.price() : priceTableItem.getPrice();
        validatePrice(price);

        Long sourcePriceTableId = priceTableItem.getPriceTable().getId();
        Long priceTableId = data.priceTableId() != null ? data.priceTableId() : sourcePriceTableId;
        PriceTable priceTable;

        if (sourcePriceTableId.equals(priceTableId)) {
            priceTable = findPriceTable(priceTableId);
        } else if (sourcePriceTableId < priceTableId) {
            findPriceTable(sourcePriceTableId);
            priceTable = findPriceTable(priceTableId);
        } else {
            priceTable = findPriceTable(priceTableId);
            findPriceTable(sourcePriceTableId);
        }

        Product product = priceTableItem.getProduct();

        if (data.productId() != null) {
            product = findProduct(data.productId());
        }
        if (Boolean.TRUE.equals(data.removePresentation()) && data.presentationId() != null) {
            throw new IllegalArgumentException(
                "Cannot provide and remove a presentation at the same time."
            );
        }
        Long presentationId = Boolean.TRUE.equals(data.removePresentation())
            ? null
            : data.presentationId() != null
              ? data.presentationId()
              : priceTableItem.getPresentation() == null
                ? null
                : priceTableItem.getPresentation().getId();
        var presentation = findPresentation(product, presentationId);
        if (repository.existsByIdentity(priceTableId, product.getId(), presentationId, id)) {
            throw new IllegalArgumentException("This product already exists in this price table.");
        }

        priceTableItem.setPriceTable(priceTable);
        priceTableItem.setProduct(product);
        priceTableItem.setPrice(price);
        priceTableItem.setPresentation(presentation);

        return repository.save(priceTableItem);
    }

    @Transactional
    public void delete(Long id) {
        PriceTableItem priceTableItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Price table item not found."));

        entityManager.refresh(priceTableItem);
        findPriceTable(priceTableItem.getPriceTable().getId());
        repository.delete(priceTableItem);
    }

    private PriceTable findPriceTable(Long priceTableId) {
        if (priceTableId == null) {
            throw new IllegalArgumentException("Price table ID is required.");
        }

        return priceTableRepository
            .findByIdForUpdate(priceTableId)
            .orElseThrow(() -> new ResourceNotFoundException("Price table not found."));
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private ProductPresentation findPresentation(Product product, Long id) {
        if (id == null) {
            return null;
        }
        var presentation = productPresentationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product presentation not found."));
        if (
            !presentation.getProductId().equals(product.getId()) ||
            presentation.isBasePresentation()
        ) {
            throw new IllegalArgumentException(
                "Use the base price row for the base presentation, or select this product's packaging."
            );
        }
        return presentation;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price is required.");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price cannot exceed 9999999999.99.");
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Price must have at most two decimal places.");
        }
    }
}
