package com.pi.erp.price.item;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.price.PriceTable;
import com.pi.erp.price.PriceTableRepository;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
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

        return repository.findAll(spec);
    }

    @Transactional
    public PriceTableItem register(RequestPriceTableItemDTO data) {
        validatePrice(data.price());
        PriceTable priceTable = findPriceTable(data.priceTableId());
        Product product = findProduct(data.productId());

        if (repository.existsByPriceTableIdAndProductId(priceTable.getId(), product.getId())) {
            throw new IllegalArgumentException("This product already exists in this price table.");
        }

        PriceTableItem priceTableItem = new PriceTableItem(data, priceTable, product);
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
        Long priceTableId = data.priceTableId() != null
            ? data.priceTableId()
            : sourcePriceTableId;
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
        if (repository.existsByPriceTableIdAndProductIdAndIdNot(priceTableId, product.getId(), id)) {
            throw new IllegalArgumentException("This product already exists in this price table.");
        }

        priceTableItem.setPriceTable(priceTable);
        priceTableItem.setProduct(product);
        priceTableItem.setPrice(price);

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
