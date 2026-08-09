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
    private Category category_id;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand_id;

    @Column(name = "cost", nullable = false, unique = true)
    private BigDecimal cost;

    @Column(name = "price", nullable = false, unique = true)
    private BigDecimal price;

    @Column(name = "weight_kg")
    private BigDecimal weight_kg;

    @Column(name = "color")
    private String color;

    @Column(name = "dimension_x")
    private BigDecimal dimension_x;

    @Column(name = "dimension_y")
    private BigDecimal dimension_y;

    @Column(name = "dimension_z")
    private BigDecimal dimension_z;

    // Faltam os dados fiscais

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at")
    private LocalDateTime updated_at;
}
