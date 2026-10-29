package com.pi.erp.price.item;

import com.pi.erp.price.PriceTable;
import com.pi.erp.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
    name = "price_table_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_price_table_item",
        columnNames = { "price_table_id", "product_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PriceTableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "price_table_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "price_table_id", nullable = false)
    private PriceTable priceTable;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    public PriceTableItem(RequestPriceTableItemDTO data, PriceTable priceTable, Product product) {
        this.priceTable = priceTable;
        this.product = product;
        this.price = data.price();
    }
}
