package com.pi.erp.stock.movement;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository
    extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement>
{
    boolean existsByPurchaseReceiptItemId(
        Long purchaseReceiptItemId
    );

    boolean existsByStockReservationId(
        Long stockReservationId
    );

    @Query(
        value = "SELECT active FROM users WHERE user_id = :userId",
        nativeQuery = true
    )
    Optional<Boolean> findUserActiveById(
        @Param("userId") Long userId
    );
}
