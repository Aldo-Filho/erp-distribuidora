package com.pi.erp.stock;

import com.pi.erp.product.Product;
import com.pi.erp.product.ProductRepository;
import com.pi.erp.warehouse.Warehouse;
import com.pi.erp.warehouse.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockItemService {
    @Autowired
    private StockItemRepository repository;

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private ProductRepository productRepository;

    public List<StockItem> search(Long warehouseId, Long productId) {
        Specification<StockItem> spec = Specification.allOf();

        if (warehouseId != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("warehouse").get("id"),
                                    warehouseId
                            )
            );
        }
        if (productId != null) {
            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("product").get("id"),
                                    productId
                            )
            );
        }
        return repository.findAll(spec);
    }

    public StockItem register(RequestStockItemDTO data) {

        if (data.quantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        if (data.reservedQuantity() < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative.");
        }
        if (data.minQuantity() < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        }
        if (data.maxQuantity() != null && data.maxQuantity() < 0) {
            throw new IllegalArgumentException("Maximum quantity cannot be negative.");
        }
        if (data.maxQuantity() != null && data.minQuantity() > data.maxQuantity()) {
            throw new IllegalArgumentException(
                    "Minimum quantity cannot be greater than maximum quantity."
            );
        }
        if (data.reservedQuantity() > data.quantity()) {
            throw new IllegalArgumentException(
                    "Reserved quantity cannot be greater than quantity."
            );
        }
        if (repository.existsByWarehouseIdAndProductId(
                data.warehouseId(),
                data.productId()
        )) {
            throw new IllegalArgumentException(
                    "This product already exists in this warehouse."
            );
        }

        Warehouse warehouse = warehouseRepository.findById(data.warehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found."));

        Product product = productRepository.findById(data.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        StockItem stockItem = new StockItem(data, warehouse, product);
        return repository.save(stockItem);
    }

    public StockItem update(Long id, PatchStockItemDTO data) {
        StockItem stockItem = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock item not found."));

        Long warehouseId = data.warehouseId() != null
                ? data.warehouseId()
                : stockItem.getWarehouse().getId();

        Long productId = data.productId() != null
                ? data.productId()
                : stockItem.getProduct().getId();

        if (repository.existsByWarehouseIdAndProductIdAndIdNot(
                warehouseId,
                productId,
                id
        )) {
            throw new IllegalArgumentException(
                    "This product already exists in this warehouse."
            );
        }

        if (data.warehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(data.warehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse not found."));

            stockItem.setWarehouse(warehouse);
        }
        if (data.productId() != null) {
            Product product = productRepository.findById(data.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found."));

            stockItem.setProduct(product);
        }
        if (data.quantity() != null) {
            if (data.quantity() < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative.");
            }
            stockItem.setQuantity(data.quantity());
        }
        if (data.reservedQuantity() != null) {
            if (data.reservedQuantity() < 0) {
                throw new IllegalArgumentException(
                        "Reserved quantity cannot be negative."
                );
            }
            stockItem.setReservedQuantity(data.reservedQuantity());
        }
        if (data.minQuantity() != null) {
            if (data.minQuantity() < 0) {
                throw new IllegalArgumentException(
                        "Minimum quantity cannot be negative."
                );
            }
            stockItem.setMinQuantity(data.minQuantity());
        }
        if (data.maxQuantity() != null) {
            if (data.maxQuantity() < 0) {
                throw new IllegalArgumentException(
                        "Maximum quantity cannot be negative."
                );
            }
            stockItem.setMaxQuantity(data.maxQuantity());
        }
        if (stockItem.getMaxQuantity() != null
                && stockItem.getMinQuantity() > stockItem.getMaxQuantity()) {
            throw new IllegalArgumentException(
                    "Minimum quantity cannot be greater than maximum quantity."
            );
        }
        if (stockItem.getReservedQuantity() > stockItem.getQuantity()) {
            throw new IllegalArgumentException(
                    "Reserved quantity cannot be greater than quantity."
            );
        }
        return repository.save(stockItem);
    }

    @Transactional
    public void delete(Long id) {
        StockItem stockItem = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock item not found."));

        repository.delete(stockItem);
    }
}

