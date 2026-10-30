package com.pi.erp.purchase.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseItemRepository
    extends JpaRepository<PurchaseItem, Long>, JpaSpecificationExecutor<PurchaseItem>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "supplierOption.productSupplier.product.catalog.brand",
            "supplierOption.productSupplier.product.catalog.category",
            "supplierOption.productSupplier.supplier",
            "warehouse",
            "purchase.supplier",
            "purchase.user.role",
        }
    )
    @Override
    Optional<PurchaseItem> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "supplierOption.productSupplier.product.catalog.brand",
            "supplierOption.productSupplier.product.catalog.category",
            "supplierOption.productSupplier.supplier",
            "warehouse",
            "purchase.supplier",
            "purchase.user.role",
        }
    )
    @Override
    List<PurchaseItem> findAll(Specification<PurchaseItem> spec);

    @Query("SELECT item.purchase.id FROM PurchaseItem item WHERE item.id = :id")
    Optional<Long> findPurchaseIdByItemId(@Param("id") Long id);

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

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "supplierOption.productSupplier.product.catalog.brand",
            "supplierOption.productSupplier.product.catalog.category",
            "supplierOption.productSupplier.supplier",
            "warehouse",
            "purchase.supplier",
            "purchase.user.role",
        }
    )
    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    boolean existsByPurchaseId(Long purchaseId);

    boolean existsByPurchaseIdAndReceivedQuantityGreaterThan(Long purchaseId, BigDecimal quantity);

    @Query(
        value = "SELECT COALESCE(SUM(ROUND(quantity * unit_cost, 2)), 0) FROM purchase_items WHERE purchase_id = :id",
        nativeQuery = true
    )
    BigDecimal calculateTotalByPurchaseId(@Param("id") Long id);

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM purchase_items WHERE purchase_id=:documentId
            AND product_id=:productId AND warehouse_id=:warehouseId
            AND supplier_option_id IS NOT DISTINCT FROM :optionId
            AND purchase_item_id<>coalesce(:id,0))
        """,
        nativeQuery = true
    )
    boolean existsByIdentity(
        @Param("documentId") Long documentId,
        @Param("productId") Long productId,
        @Param("warehouseId") Long warehouseId,
        @Param("optionId") Long optionId,
        @Param("id") Long id
    );
}
