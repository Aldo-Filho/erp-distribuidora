package com.pi.erp.stock;

import com.pi.erp.product.Product;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @Column(name = "quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "min_quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal minQuantity = BigDecimal.ZERO;

    @Column(name = "max_quantity", precision = 14, scale = 1)
    private BigDecimal maxQuantity;

    public StockItem(RequestStockItemDTO data, Warehouse warehouse, Product product) {
        this.warehouse = warehouse;
        this.product = product;
        this.quantity = data.quantity() != null ? data.quantity() : BigDecimal.ZERO;
        this.reservedQuantity =
            data.reservedQuantity() != null ? data.reservedQuantity() : BigDecimal.ZERO;
        this.minQuantity = data.minQuantity() != null ? data.minQuantity() : BigDecimal.ZERO;
        this.maxQuantity = data.maxQuantity();
    }
}
