package com.pi.erp.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockItemRepository extends JpaRepository<StockItem, Long>, JpaSpecificationExecutor<StockItem> {
    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);
    boolean existsByWarehouseIdAndProductIdAndIdNot(Long warehouseId, Long productId, Long id);
}
