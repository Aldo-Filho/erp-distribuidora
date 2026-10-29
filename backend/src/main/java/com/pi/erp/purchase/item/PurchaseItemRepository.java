package com.pi.erp.purchase.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseItemRepository
    extends JpaRepository<PurchaseItem, Long>, JpaSpecificationExecutor<PurchaseItem>
{
    @Query("SELECT item.purchase.id FROM PurchaseItem item WHERE item.id = :id")
    Optional<Long> findPurchaseIdByItemId(
        @Param("id") Long id
    );

    @Query(
        value = """
            SELECT * FROM purchase_items
            WHERE purchase_item_id = :id AND purchase_id = :purchaseId FOR UPDATE
            """,
        nativeQuery = true
    )
    Optional<PurchaseItem> findByIdAndPurchaseIdForUpdate(
        @Param("id") Long id,
        @Param("purchaseId") Long purchaseId
    );

    List<PurchaseItem> findByPurchaseId(
        Long purchaseId
    );

    boolean existsByPurchaseId(
        Long purchaseId
    );

    boolean existsByPurchaseIdAndReceivedQuantityGreaterThan(
        Long purchaseId,
        Integer quantity
    );

    boolean existsByPurchaseIdAndProductId(
        Long purchaseId,
        Long productId
    );

    boolean existsByPurchaseIdAndProductIdAndIdNot(
        Long purchaseId,
        Long productId,
        Long id
    );

    @Query(
        value = "SELECT COALESCE(SUM(quantity * unit_cost), 0) FROM purchase_items WHERE purchase_id = :id",
        nativeQuery = true
    )
    BigDecimal calculateTotalByPurchaseId(
        @Param("id") Long id
    );
}
