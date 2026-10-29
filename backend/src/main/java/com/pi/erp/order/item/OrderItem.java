package com.pi.erp.order.item;

import com.pi.erp.order.Order;
import com.pi.erp.product.Product;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.AMOUNT;

    public OrderItem(
        RequestOrderItemDTO data,
        Order order,
        Product product,
        Warehouse warehouse,
        BigDecimal unitPrice
    ) {
        this.order = order;
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = data.quantity();
        this.unitPrice = unitPrice;
        this.discount = data.discount() == null ? BigDecimal.ZERO : data.discount();
        this.discountType = data.discountType() == null ? DiscountType.AMOUNT : data.discountType();
    }
}
