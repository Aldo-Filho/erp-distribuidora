package com.pi.erp.purchase.receipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "items.purchaseItem", "items.stockItem" }
    )
    Optional<PurchaseReceipt> findByPurchaseIdAndRequestId(
        Long purchaseId,
        UUID requestId
    );

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "items.purchaseItem", "items.stockItem" }
    )
    List<PurchaseReceipt> findByPurchaseIdOrderByIdAsc(
        Long purchaseId
    );

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "items.purchaseItem", "items.stockItem" }
    )
    Optional<PurchaseReceipt> findByIdAndPurchaseId(
        Long id,
        Long purchaseId
    );
}
