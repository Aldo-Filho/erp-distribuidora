package com.pi.erp.supplier.category;

import com.pi.erp.product.category.Category;
import com.pi.erp.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier_categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SupplierCategory {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private SupplierCategoryId id = new SupplierCategoryId();

    @MapsId("supplierId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @MapsId("categoryId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public SupplierCategory(Supplier supplier, Category category) {
        this.id = new SupplierCategoryId(supplier.getId(), category.getId());
        this.supplier = supplier;
        this.category = category;
    }
}
