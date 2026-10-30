package com.pi.erp.purchase.item;

import com.pi.erp.product.Product;
import com.pi.erp.product.ProductAttributesConverter;
import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductQuantity;
import com.pi.erp.product.ProductUnit;
import com.pi.erp.purchase.Purchase;
import com.pi.erp.supplier.product.option.ProductSupplierOption;
import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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

    @ManyToOne
    @JoinColumn(name = "supplier_option_id")
    private ProductSupplierOption supplierOption;

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

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "received_quantity", nullable = false, precision = 14, scale = 1)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

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

    public void captureSnapshot(ProductSupplierOption selected) {
        this.supplierOption = selected;
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
            Boolean.TRUE.equals(selected.getBasePresentation())
        );
    }

    public void updateBaseQuantity() {
        this.baseQuantity = ProductQuantity.toBase(
            quantity,
            baseFactorSnapshot,
            unitSnapshot != null ? unitSnapshot : product.getUnit(),
            supplierOption == null || Boolean.TRUE.equals(supplierOption.getBasePresentation())
        );
    }
}
