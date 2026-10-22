package com.pi.erp.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.category.Category;
import com.pi.erp.supplier.product.ProductSupplier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

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

    public Product(RequestProductDTO data, Brand brand, Category category) {
        this.name = data.name();
        // TODO: Generate the SKU automatically.
        this.sku = data.sku();
        this.brand = brand;
        this.category = category;
        this.cost = data.cost();
        this.price = data.price();
        this.weightKg = data.weightKg();
        this.color = data.color();
        this.dimensionX = data.dimensionX();
        this.dimensionY = data.dimensionY();
        this.dimensionZ = data.dimensionZ();
        this.size = data.size();
        this.active = data.active() == null || data.active();
    }
}
