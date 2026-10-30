package com.pi.erp.stock.reservation;

import com.pi.erp.order.item.OrderItem;
import com.pi.erp.stock.StockItem;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "stock_reservations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_reservation_order_item",
        columnNames = { "order_item_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "stock_reservation_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public StockReservation(OrderItem orderItem, StockItem stockItem) {
        this.orderItem = orderItem;
        this.stockItem = stockItem;
        this.quantity = orderItem.getBaseQuantity();
        this.status = StockReservationStatus.ACTIVE;
    }
}
