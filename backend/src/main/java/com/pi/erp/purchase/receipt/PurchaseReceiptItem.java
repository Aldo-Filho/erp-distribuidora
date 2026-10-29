package com.pi.erp.purchase.receipt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.purchase.item.PurchaseItem;
import com.pi.erp.stock.StockItem;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "purchase_receipt_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_purchase_receipt_item",
        columnNames = { "purchase_receipt_id", "purchase_item_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PurchaseReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "purchase_receipt_item_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "purchase_receipt_id", nullable = false)
    private PurchaseReceipt receipt;

    @ManyToOne
    @JoinColumn(name = "purchase_item_id", nullable = false)
    private PurchaseItem purchaseItem;

    @ManyToOne
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public PurchaseReceiptItem(
        PurchaseReceipt receipt,
        PurchaseItem purchaseItem,
        StockItem stockItem,
        Integer quantity
    ) {
        this.receipt = receipt;
        this.purchaseItem = purchaseItem;
        this.stockItem = stockItem;
        this.quantity = quantity;
    }
}
