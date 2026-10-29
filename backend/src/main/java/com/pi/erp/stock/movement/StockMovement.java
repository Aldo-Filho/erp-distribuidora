package com.pi.erp.stock.movement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.purchase.receipt.PurchaseReceiptItem;
import com.pi.erp.stock.StockItem;
import com.pi.erp.stock.reservation.StockReservation;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "stock_movement_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private StockMovementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30)
    private StockMovementReason reason;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_balance", nullable = false)
    private Integer previousBalance;

    @Column(name = "new_balance", nullable = false)
    private Integer newBalance;

    // Scalar origin IDs preserve the movement response contract.
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "purchase_id")
    private Long purchaseId;

    // The operator is supplied by the caller until authentication is implemented.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "notes")
    private String notes;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "stock_reservation_id", unique = true)
    private StockReservation stockReservation;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "purchase_receipt_item_id", unique = true)
    private PurchaseReceiptItem purchaseReceiptItem;

    @PrePersist
    public void prePersist() {
        occurredAt = LocalDateTime.now();
    }

    public StockMovement(
        RequestStockMovementDTO data,
        StockItem stockItem,
        Integer previousBalance,
        Integer newBalance,
        String notes
    ) {
        this.stockItem = stockItem;
        this.type = data.type();
        this.reason = data.reason();
        this.quantity = data.quantity();
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.userId = data.userId();
        this.notes = notes;
    }

    public StockMovement(
        PurchaseReceiptItem receiptItem,
        Integer previousBalance,
        Integer newBalance
    ) {
        this.stockItem = receiptItem.getStockItem();
        this.type = StockMovementType.INBOUND;
        this.reason = StockMovementReason.PURCHASE;
        this.quantity = receiptItem.getQuantity();
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.purchaseId = receiptItem.getReceipt().getPurchase().getId();
        this.userId = receiptItem.getReceipt().getOperator().getId();
        this.notes = receiptItem.getReceipt().getNotes();
        this.purchaseReceiptItem = receiptItem;
    }

    public StockMovement(
        StockReservation reservation,
        Long operatorUserId,
        Integer previousBalance,
        Integer newBalance,
        String notes
    ) {
        this.stockItem = reservation.getStockItem();
        this.type = StockMovementType.OUTBOUND;
        this.reason = StockMovementReason.SALE;
        this.quantity = reservation.getQuantity();
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.orderId = reservation.getOrderItem().getOrder().getId();
        this.userId = operatorUserId;
        this.stockReservation = reservation;
        this.notes = notes;
    }
}
