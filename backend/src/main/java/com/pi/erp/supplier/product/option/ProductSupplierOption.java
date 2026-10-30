package com.pi.erp.supplier.product.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.supplier.product.ProductSupplier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "product_supplier_options")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductSupplierOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_supplier_option_id")
    @EqualsAndHashCode.Include
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "product_supplier_id", nullable = false)
    private ProductSupplier productSupplier;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_factor", precision = 14, scale = 1)
    private BigDecimal baseFactor;

    @Column(name = "base_presentation")
    private Boolean basePresentation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "avg_delivery_days")
    private Integer avgDeliveryDays;

    @Column(name = "supplier_product_code")
    private String supplierProductCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "conversion_confirmed", nullable = false)
    private boolean conversionConfirmed;

    public Long getProductSupplierId() {
        return productSupplier.getId();
    }

    public Integer getEffectiveAvgDeliveryDays() {
        return avgDeliveryDays != null
            ? avgDeliveryDays
            : productSupplier.getSupplier().getAvgDeliveryDays();
    }
}
