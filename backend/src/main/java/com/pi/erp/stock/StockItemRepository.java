package com.pi.erp.stock;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository
    extends JpaRepository<StockItem, Long>, JpaSpecificationExecutor<StockItem>
{
    @Query(
        value = "SELECT * FROM stock_items WHERE stock_item_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<StockItem> findByIdForUpdate(
        @Param("id") Long id
    );

    @Query("""
        SELECT stockItem.id FROM StockItem stockItem
        WHERE stockItem.warehouse.id = :warehouseId AND stockItem.product.id = :productId
        """)
    Optional<Long> findIdByWarehouseIdAndProductId(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId
    );

    boolean existsByWarehouseIdAndProductId(
        Long warehouseId,
        Long productId
    );

    boolean existsByWarehouseIdAndProductIdAndIdNot(
        Long warehouseId,
        Long productId,
        Long id
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM stock_movements
                WHERE stock_item_id = :id
            )
            """,
        nativeQuery = true
    )
    boolean existsMovementsByStockItemId(
        @Param("id") Long id
    );
}
