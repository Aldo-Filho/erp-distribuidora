package com.pi.erp.purchase.item;

import com.pi.erp.product.Product;
import com.pi.erp.purchase.Purchase;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "purchase_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "purchase_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    public PurchaseItem(
        RequestPurchaseItemDTO data,
        Purchase purchase,
        Product product,
        Warehouse warehouse,
        BigDecimal unitCost
    ) {
        this.purchase = purchase;
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = data.quantity();
        this.unitCost = unitCost;
    }
}
