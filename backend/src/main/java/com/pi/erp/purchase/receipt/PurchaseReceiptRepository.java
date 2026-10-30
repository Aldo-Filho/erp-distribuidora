package com.pi.erp.purchase.receipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "purchase.supplier",
            "purchase.user.role",
            "operator.role",
            "items.purchaseItem.product.catalog.brand",
            "items.purchaseItem.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.brand",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.supplier",
            "items.purchaseItem.purchase.supplier",
            "items.purchaseItem.purchase.user.role",
            "items.purchaseItem.warehouse",
            "items.stockItem.product.catalog.brand",
            "items.stockItem.product.catalog.category",
            "items.stockItem.warehouse",
        }
    )
    Optional<PurchaseReceipt> findByPurchaseIdAndRequestId(Long purchaseId, UUID requestId);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "purchase.supplier",
            "purchase.user.role",
            "operator.role",
            "items.purchaseItem.product.catalog.brand",
            "items.purchaseItem.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.brand",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.supplier",
            "items.purchaseItem.purchase.supplier",
            "items.purchaseItem.purchase.user.role",
            "items.purchaseItem.warehouse",
            "items.stockItem.product.catalog.brand",
            "items.stockItem.product.catalog.category",
            "items.stockItem.warehouse",
        }
    )
    List<PurchaseReceipt> findByPurchaseIdOrderByIdAsc(Long purchaseId);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "purchase.supplier",
            "purchase.user.role",
            "operator.role",
            "items.purchaseItem.product.catalog.brand",
            "items.purchaseItem.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.brand",
            "items.purchaseItem.supplierOption.productSupplier.product.catalog.category",
            "items.purchaseItem.supplierOption.productSupplier.supplier",
            "items.purchaseItem.purchase.supplier",
            "items.purchaseItem.purchase.user.role",
            "items.purchaseItem.warehouse",
            "items.stockItem.product.catalog.brand",
            "items.stockItem.product.catalog.category",
            "items.stockItem.warehouse",
        }
    )
    Optional<PurchaseReceipt> findByIdAndPurchaseId(Long id, Long purchaseId);
}
