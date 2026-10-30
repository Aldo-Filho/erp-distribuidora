package com.pi.erp.order.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository
    extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "presentation.product.catalog.brand",
            "presentation.product.catalog.category",
            "warehouse",
            "order.customer.priceTable",
            "order.user.role",
        }
    )
    @Override
    Optional<OrderItem> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "presentation.product.catalog.brand",
            "presentation.product.catalog.category",
            "warehouse",
            "order.customer.priceTable",
            "order.user.role",
        }
    )
    @Override
    List<OrderItem> findAll(Specification<OrderItem> spec);

    @Query(
        value = "SELECT * FROM order_items WHERE order_item_id = :id FOR NO KEY UPDATE",
        nativeQuery = true
    )
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT item.order.id FROM OrderItem item WHERE item.id = :id")
    Optional<Long> findOrderIdByItemId(@Param("id") Long id);

    boolean existsByOrderId(Long orderId);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "presentation.product.catalog.brand",
            "presentation.product.catalog.category",
            "warehouse",
            "order.customer.priceTable",
            "order.user.role",
        }
    )
    List<OrderItem> findByOrderId(Long orderId);

    @Query(
        value = """
            SELECT COALESCE(SUM(
            ROUND(quantity * unit_price, 2) - CASE discount_type
                WHEN 'PERCENTAGE' THEN ROUND(ROUND(quantity * unit_price, 2) * discount / 100, 2)
                ELSE discount
            END
            ), 0)
            FROM order_items
            WHERE order_id = :orderId
        """,
        nativeQuery = true
    )
    BigDecimal calculateTotalByOrderId(@Param("orderId") Long orderId);

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM order_items WHERE order_id=:documentId
            AND product_id=:productId AND warehouse_id=:warehouseId
            AND presentation_id IS NOT DISTINCT FROM :optionId
            AND order_item_id<>coalesce(:id,0))
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
