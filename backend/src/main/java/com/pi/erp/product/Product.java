package com.pi.erp.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.catalog.ProductCatalog;
import com.pi.erp.product.catalog.ProductCatalogType;
import com.pi.erp.product.category.Category;
import com.pi.erp.supplier.product.ProductSupplier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "product_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "catalog_id", nullable = false)
    private ProductCatalog catalog;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "product_variant_values",
        joinColumns = @JoinColumn(name = "product_id")
    )
    @MapKeyColumn(name = "characteristic")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "value")
    private Map<ProductCharacteristic, String> attributes = new HashMap<>();

    @JsonIgnore
    @Column(name = "variant_key", nullable = false)
    private String variantKey = "";

    @JsonIgnore
    @OneToMany(mappedBy = "product")
    private List<ProductSupplier> productSuppliers;

    @Column(name = "cost", nullable = false)
    private BigDecimal cost;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "color")
    private String color;

    @Column(name = "dimension_x")
    private BigDecimal dimensionX;

    @Column(name = "dimension_y")
    private BigDecimal dimensionY;

    @Column(name = "dimension_z")
    private BigDecimal dimensionZ;

    @Column(name = "size")
    private String size;

    // TODO: Map the tax details.

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getCatalogId() {
        return catalog.getId();
    }

    public String getName() {
        return catalog.getName();
    }

    public Brand getBrand() {
        return catalog.getBrand();
    }

    public Category getCategory() {
        return catalog.getCategory();
    }

    public ProductUnit getUnit() {
        return catalog.getUnit();
    }

    public boolean isActive() {
        return catalog.isActive() && active;
    }

    public boolean getOwnActive() {
        return active;
    }

    public String getColor() {
        return catalog.getType() == ProductCatalogType.VARIANT
            ? attributes.get(ProductCharacteristic.COLOR)
            : color;
    }

    public String getSize() {
        return catalog.getType() == ProductCatalogType.VARIANT
            ? attributes.get(ProductCharacteristic.SIZE)
            : size;
    }
}
