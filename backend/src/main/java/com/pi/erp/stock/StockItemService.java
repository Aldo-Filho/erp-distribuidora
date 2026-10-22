package com.pi.erp.stock;

import com.pi.erp.exception.ResourceNotFoundException;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<StockItem> search(StockItemFilter filter) {
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

        return repository.findAll(spec);
    }

    public StockItem register(RequestStockItemDTO data) {
        validateQuantities(
            data.quantity(),
            data.reservedQuantity(),
            data.minQuantity(),
            data.maxQuantity()
        );

        if (repository.existsByWarehouseIdAndProductId(data.warehouseId(), data.productId())) {
            throw new IllegalArgumentException("This product already exists in this warehouse.");
        }

        Warehouse warehouse = findWarehouse(data.warehouseId());
        Product product = findProduct(data.productId());

        StockItem stockItem = new StockItem(data, warehouse, product);
        return repository.save(stockItem);
    }

    @Transactional
    public StockItem update(Long id, PatchStockItemDTO data) {
        StockItem stockItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

        if (Boolean.TRUE.equals(data.removeMaxQuantity()) && data.maxQuantity() != null) {
            throw new IllegalArgumentException(
                "Cannot provide a maximum quantity and request its removal at the same time."
            );
        }

        Long warehouseId = data.warehouseId() != null
            ? data.warehouseId()
            : stockItem.getWarehouse().getId();
        Long productId = data.productId() != null
            ? data.productId()
            : stockItem.getProduct().getId();

        if ((!warehouseId.equals(stockItem.getWarehouse().getId())
            || !productId.equals(stockItem.getProduct().getId()))
            && repository.existsMovementsByStockItemId(id)) {
            throw new IllegalArgumentException(
                "Cannot change the product or warehouse of a stock item with movements."
            );
        }

        Integer quantity = data.quantity() != null ? data.quantity() : stockItem.getQuantity();
        Integer reservedQuantity = data.reservedQuantity() != null
            ? data.reservedQuantity()
            : stockItem.getReservedQuantity();
        Integer minQuantity = data.minQuantity() != null
            ? data.minQuantity()
            : stockItem.getMinQuantity();
        Integer maxQuantity = stockItem.getMaxQuantity();

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
        StockItem stockItem = repository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Stock item not found."));

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

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        return productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private void validateQuantities(
        Integer quantity,
        Integer reservedQuantity,
        Integer minQuantity,
        Integer maxQuantity
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
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        if (reservedQuantity < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative.");
        }
        if (minQuantity < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        }
        if (maxQuantity != null && maxQuantity < 0) {
            throw new IllegalArgumentException("Maximum quantity cannot be negative.");
        }
        if (maxQuantity != null && minQuantity > maxQuantity) {
            throw new IllegalArgumentException(
                "Minimum quantity cannot be greater than maximum quantity."
            );
        }
        if (reservedQuantity > quantity) {
            throw new IllegalArgumentException(
                "Reserved quantity cannot be greater than quantity."
            );
        }
    }
}
