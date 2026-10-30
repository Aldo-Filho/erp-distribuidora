package com.pi.erp.order.item;

import com.pi.erp.order.Order;
import com.pi.erp.product.Product;
import com.pi.erp.product.ProductAttributesConverter;
import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductUnit;
import com.pi.erp.product.presentation.ProductPresentation;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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

    @ManyToOne
    @JoinColumn(name = "presentation_id")
    private ProductPresentation presentation;

    @Column(name = "base_quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal baseQuantity;

    @Column(name = "presentation_name_snapshot")
    private String presentationNameSnapshot;

    @Column(name = "base_factor_snapshot", nullable = false, precision = 14, scale = 1)
    private BigDecimal baseFactorSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_snapshot")
    private ProductUnit unitSnapshot;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "sku_snapshot")
    private String skuSnapshot;

    @Convert(converter = ProductAttributesConverter.class)
    @Column(name = "attributes_snapshot", columnDefinition = "text")
    private Map<ProductCharacteristic, String> attributesSnapshot;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal quantity;

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

    public void captureSnapshot(ProductPresentation selected) {
        this.presentation = selected;
        this.presentationNameSnapshot = selected.getName();
        this.baseFactorSnapshot = selected.getBaseFactor();
        this.unitSnapshot = product.getUnit();
        this.productNameSnapshot = product.getName();
        this.skuSnapshot = product.getSku();
        this.attributesSnapshot = new HashMap<>(product.getAttributes());
        this.baseQuantity = ProductQuantity.toBase(
            quantity,
            baseFactorSnapshot,
            unitSnapshot,
            selected.isBasePresentation()
        );
    }

    public void updateBaseQuantity() {
        this.baseQuantity = ProductQuantity.toBase(
            quantity,
            baseFactorSnapshot,
            unitSnapshot != null ? unitSnapshot : product.getUnit(),
            presentation == null || presentation.isBasePresentation()
        );
    }
}
