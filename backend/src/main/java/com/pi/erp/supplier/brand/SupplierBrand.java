package com.pi.erp.supplier.brand;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier_brands")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SupplierBrand {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private SupplierBrandId id = new SupplierBrandId();

    @MapsId("supplierId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @MapsId("brandId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    public SupplierBrand(Supplier supplier, Brand brand) {
        this.id = new SupplierBrandId(supplier.getId(), brand.getId());
        this.supplier = supplier;
        this.brand = brand;
    }
}
