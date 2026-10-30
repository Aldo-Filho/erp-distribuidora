package com.pi.erp.stock;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.product.catalog.ProductCatalogRepository;
import com.pi.erp.stock.reservation.StockReservationRepository;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockItemService {

    @Autowired
    private StockItemRepository repository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private ProductCatalogRepository productCatalogRepository;

    @Autowired
    private EntityManager entityManager;

    public Page<StockItem> search(StockItemFilter filter, Pageable pageable) {
        Specification<StockItem> spec = Specification.allOf();

        if (filter.warehouseId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), filter.warehouseId())
            );
        }
        if (filter.productId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("product").get("id"), filter.productId())
            );
        }

        return repository.findAll(spec, pageable);
    }

    @Transactional
    public StockItem register(RequestStockItemDTO data) {
        validateQuantities(
            data.quantity(),
            data.reservedQuantity(),
            data.minQuantity(),
            data.maxQuantity()
        );

        if (data.reservedQuantity().signum() != 0) {
            throw new IllegalArgumentException("Initial reserved quantity must be zero.");
        }

        if (repository.existsByWarehouseIdAndProductId(data.warehouseId(), data.productId())) {
            throw new IllegalArgumentException("This product already exists in this warehouse.");
        }

        Warehouse warehouse = findWarehouse(data.warehouseId());
        Product product = lockProduct(data.productId());

        validateUnitQuantities(
            product,
            data.quantity(),
            data.reservedQuantity(),
            data.minQuantity(),
            data.maxQuantity()
        );
        StockItem stockItem = new StockItem(data, warehouse, product);
        return repository.save(stockItem);
    }

    @Transactional
    public StockItem update(Long id, PatchStockItemDTO data) {
        StockItem observed = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));
        Long originalProductId = observed.getProduct().getId();
        Long targetProductId = data.productId() != null ? data.productId() : originalProductId;
        lockProducts(Stream.of(originalProductId, targetProductId).distinct().toList());
        StockItem stockItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

        entityManager.refresh(stockItem);
        if (!stockItem.getProduct().getId().equals(originalProductId)) {
            throw new IllegalArgumentException("Stock item changed. Retry the operation.");
        }

        if (
            data.reservedQuantity() != null &&
                data.reservedQuantity().compareTo(stockItem.getReservedQuantity()) != 0
        ) {
            throw new IllegalArgumentException(
                "Reserved quantity is managed by order reservations."
            );
        }

        if (data.quantity() != null && data.quantity().compareTo(stockItem.getQuantity()) != 0) {
            throw new IllegalArgumentException("Use a stock movement to change quantity.");
        }

        if (Boolean.TRUE.equals(data.removeMaxQuantity()) && data.maxQuantity() != null) {
            throw new IllegalArgumentException(
                "Cannot provide a maximum quantity and request its removal at the same time."
            );
        }

        Long warehouseId =
            data.warehouseId() != null ? data.warehouseId() : stockItem.getWarehouse().getId();
        Long productId =
            data.productId() != null ? data.productId() : stockItem.getProduct().getId();

        if (
            (!warehouseId.equals(stockItem.getWarehouse().getId()) ||
                !productId.equals(stockItem.getProduct().getId())) &&
                repository.existsMovementsByStockItemId(id)
        ) {
            throw new IllegalArgumentException(
                "Cannot change the product or warehouse of a stock item with movements."
            );
        }

        if (
            (!warehouseId.equals(stockItem.getWarehouse().getId()) ||
                !productId.equals(stockItem.getProduct().getId())) &&
                stockReservationRepository.existsByStockItemId(id)
        ) {
            throw new IllegalArgumentException(
                "Cannot change the product or warehouse of a stock item with reservations."
            );
        }

        BigDecimal quantity = data.quantity() != null ? data.quantity() : stockItem.getQuantity();
        BigDecimal reservedQuantity =
            data.reservedQuantity() != null
                ? data.reservedQuantity()
                : stockItem.getReservedQuantity();
        BigDecimal minQuantity =
            data.minQuantity() != null ? data.minQuantity() : stockItem.getMinQuantity();
        BigDecimal maxQuantity = stockItem.getMaxQuantity();

        if (Boolean.TRUE.equals(data.removeMaxQuantity())) {
            maxQuantity = null;
        } else if (data.maxQuantity() != null) {
            maxQuantity = data.maxQuantity();
        }

        validateQuantities(quantity, reservedQuantity, minQuantity, maxQuantity);

        if (repository.existsByWarehouseIdAndProductIdAndIdNot(warehouseId, productId, id)) {
            throw new IllegalArgumentException("This product already exists in this warehouse.");
        }

        Warehouse warehouse = stockItem.getWarehouse();
        Product product = stockItem.getProduct();

        if (data.warehouseId() != null) {
            warehouse = findWarehouse(data.warehouseId());
        }
        if (data.productId() != null) {
            product = findProduct(data.productId());
        }

        validateUnitQuantities(product, quantity, reservedQuantity, minQuantity, maxQuantity);
        stockItem.setWarehouse(warehouse);
        stockItem.setProduct(product);
        stockItem.setQuantity(quantity);
        stockItem.setReservedQuantity(reservedQuantity);
        stockItem.setMinQuantity(minQuantity);
        stockItem.setMaxQuantity(maxQuantity);

        return repository.save(stockItem);
    }

    @Transactional
    public void delete(Long id) {
        StockItem observed = repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));
        Long originalProductId = observed.getProduct().getId();
        lockProduct(originalProductId);
        StockItem stockItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

        entityManager.refresh(stockItem);

        if (!stockItem.getProduct().getId().equals(originalProductId)) {
            throw new IllegalArgumentException("Stock item changed. Retry the operation.");
        }
        if (stockReservationRepository.existsByStockItemId(id)) {
            throw new IllegalArgumentException("Cannot delete a stock item with reservations.");
        }

        if (repository.existsMovementsByStockItemId(id)) {
            throw new IllegalArgumentException("Cannot delete a stock item with movements.");
        }

        repository.delete(stockItem);
    }

    private Warehouse findWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID is required.");
        }

        return warehouseRepository
            .findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found."));
    }

    private Product lockProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        lockProducts(List.of(productId));
        return findProduct(productId);
    }

    private void lockProducts(List<Long> ids) {
        Map<Long, Long> catalogs = new TreeMap<>();
        for (Long id : ids) {
            catalogs.put(
                id,
                productRepository
                    .findCatalogId(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found."))
            );
        }
        catalogs
            .values()
            .stream()
            .distinct()
            .sorted()
            .forEach(id -> {
                var catalog = productCatalogRepository
                    .findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product catalog not found."));
                entityManager.refresh(catalog);
            });
        for (Long id : catalogs.keySet()) {
            Product product = productRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
            entityManager.refresh(product);
        }
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private void validateUnitQuantities(
        Product product,
        BigDecimal quantity,
        BigDecimal reserved,
        BigDecimal min,
        BigDecimal max
    ) {
        ProductQuantity.requireBase(quantity, product.getUnit(), true);
        ProductQuantity.requireBase(reserved, product.getUnit(), true);
        ProductQuantity.requireBase(min, product.getUnit(), true);
        if (max != null) {
            ProductQuantity.requireBase(max, product.getUnit(), true);
        }
    }

    private void validateQuantities(
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal minQuantity,
        BigDecimal maxQuantity
    ) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required.");
        }
        if (reservedQuantity == null) {
            throw new IllegalArgumentException("Reserved quantity is required.");
        }
        if (minQuantity == null) {
            throw new IllegalArgumentException("Minimum quantity is required.");
        }
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        if (reservedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative.");
        }
        if (minQuantity.signum() < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        }
        if (maxQuantity != null && maxQuantity.signum() < 0) {
            throw new IllegalArgumentException("Maximum quantity cannot be negative.");
        }
        if (maxQuantity != null && minQuantity.compareTo(maxQuantity) > 0) {
            throw new IllegalArgumentException(
                "Minimum quantity cannot be greater than maximum quantity."
            );
        }
        if (reservedQuantity.compareTo(quantity) > 0) {
            throw new IllegalArgumentException(
                "Reserved quantity cannot be greater than quantity."
            );
        }
    }
}
