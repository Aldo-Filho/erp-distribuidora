package com.pi.erp.stock;

import com.pi.erp.product.Product;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "stock_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_item",
        columnNames = { "warehouse_id", "product_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "stock_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 0;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    public StockItem(
        RequestStockItemDTO data,
        Warehouse warehouse,
        Product product
    ) {
        this.warehouse = warehouse;
        this.product = product;
        this.quantity = data.quantity() != null ? data.quantity() : 0;
        this.reservedQuantity = data.reservedQuantity() != null ? data.reservedQuantity() : 0;
        this.minQuantity = data.minQuantity() != null ? data.minQuantity() : 0;
        this.maxQuantity = data.maxQuantity();
    }
}
