package com.pi.erp.order.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository
    extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem>
{
    @Query(
        value = "SELECT * FROM order_items WHERE order_item_id = :id FOR NO KEY UPDATE",
        nativeQuery = true
    )
    Optional<OrderItem> findByIdForUpdate(
        @Param("id") Long id
    );

    boolean existsByOrderId(
        Long orderId
    );

    List<OrderItem> findByOrderId(
        Long orderId
    );

    boolean existsByOrderIdAndProductId(
        Long orderId,
        Long productId
    );

    boolean existsByOrderIdAndProductIdAndIdNot(
        Long orderId,
        Long productId,
        Long id
    );

    @Query(
        value = """
            SELECT COALESCE(SUM(
                quantity * unit_price - CASE discount_type
                    WHEN 'PERCENTAGE' THEN ROUND(quantity * unit_price * discount / 100, 2)
                    ELSE discount
                END
            ), 0)
            FROM order_items
            WHERE order_id = :orderId
            """,
        nativeQuery = true
    )
    BigDecimal calculateTotalByOrderId(
        @Param("orderId") Long orderId
    );
}
