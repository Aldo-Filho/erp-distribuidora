package com.pi.erp.product.category;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.catalog.ProductCatalog;
import com.pi.erp.supplier.category.SupplierCategory;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "category_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<ProductCatalog> catalogs;

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<SupplierCategory> supplierCategories;

    public Category(RequestCategoryDTO data) {
        this.name = data.name();
    }
}
