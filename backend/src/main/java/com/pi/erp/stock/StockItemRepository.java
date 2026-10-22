package com.pi.erp.stock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository
    extends JpaRepository<StockItem, Long>, JpaSpecificationExecutor<StockItem>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT stockItem FROM StockItem stockItem WHERE stockItem.id = :id")
    Optional<StockItem> findByIdForUpdate(
        @Param("id") Long id
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
