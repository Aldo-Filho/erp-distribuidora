package com.pi.erp.supplier.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.Product;
import com.pi.erp.supplier.Supplier;
import com.pi.erp.supplier.product.option.ProductSupplierOption;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
    name = "product_suppliers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_supplier",
        columnNames = { "product_id", "supplier_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "product_supplier_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "legacy_option_id")
    private ProductSupplierOption legacyOption;

    @Column(name = "preferred", nullable = false)
    private boolean preferred = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public ProductSupplier(RequestProductSupplierDTO data, Product product, Supplier supplier) {
        this.product = product;
        this.supplier = supplier;
        this.preferred = Boolean.TRUE.equals(data.preferred());
        this.active = data.active() == null || data.active();
    }

    public Long getLegacyOptionId() {
        return legacyOption == null ? null : legacyOption.getId();
    }

    public BigDecimal getCost() {
        return legacyOption == null ? null : legacyOption.getCost();
    }

    public Integer getAvgDeliveryDays() {
        return legacyOption == null ? null : legacyOption.getAvgDeliveryDays();
    }

    public String getSupplierProductCode() {
        return legacyOption == null ? null : legacyOption.getSupplierProductCode();
    }
}
