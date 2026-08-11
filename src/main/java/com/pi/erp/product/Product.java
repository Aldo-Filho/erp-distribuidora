package com.pi.erp.product;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @JoinColumn(name = "brand_id",  nullable = false)
    private Brand brand;

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

    // Faltam os dados fiscais

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

    public Product(RequestProductDTO requestProductDTO, Brand brand, Category category) {
        this.name = requestProductDTO.name();
        // Ajustar SKU para geração automática
        this.sku =  requestProductDTO.sku();
        this.brand = brand;
        this.category = category;
        this.cost = requestProductDTO.cost();
        this.price = requestProductDTO.price();
        this.weightKg = requestProductDTO.weightKg();
        this.color = requestProductDTO.color();
        this.dimensionX = requestProductDTO.dimensionX();
        this.dimensionY = requestProductDTO.dimensionY();
        this.dimensionZ = requestProductDTO.dimensionZ();
        this.active = true;
    }
}
