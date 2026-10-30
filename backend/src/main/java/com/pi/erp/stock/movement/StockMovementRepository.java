package com.pi.erp.stock.movement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository
    extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement>
{
    Optional<StockMovement> findByRequestId(UUID requestId);

    @Query(
        value = "SELECT 1 FROM pg_advisory_xact_lock(:requestKey)",
        nativeQuery = true
    )
    Integer lockRequest(
        @Param("requestKey") long requestKey
    );

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
