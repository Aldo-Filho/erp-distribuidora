package com.pi.erp.stock.reservation;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    @Query(
        """
        SELECT reservation FROM StockReservation reservation
        WHERE reservation.orderItem.order.id = :orderId
        """
    )
    List<StockReservation> findByOrderId(@Param("orderId") Long orderId);

    boolean existsByStockItemId(Long stockItemId);

    @Query(
        value = """
        SELECT COALESCE(SUM(quantity), 0) FROM stock_reservations
        WHERE stock_item_id = :stockItemId AND status = 'ACTIVE'
        """,
        nativeQuery = true
    )
    BigDecimal sumActiveQuantityByStockItemId(@Param("stockItemId") Long stockItemId);
}
