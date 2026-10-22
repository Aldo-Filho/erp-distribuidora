package com.pi.erp.product.brand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.Product;
import com.pi.erp.supplier.brand.SupplierBrand;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "brands")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "brand_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "brand")
    private List<Product> products;

    @JsonIgnore
    @OneToMany(mappedBy = "brand")
    private List<SupplierBrand> supplierBrands;

    public Brand(RequestBrandDTO data) {
        this.name = data.name();
    }
}
