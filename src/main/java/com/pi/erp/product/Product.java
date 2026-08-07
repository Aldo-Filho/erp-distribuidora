package com.pi.erp.product;

import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    private Long product_id;

    @Column(name = "name")
    private String name;

    @Column(name = "sku")
    private String sku;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category_id;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand_id;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "weight_kg")
    private BigDecimal weight_kg;

    @Column(name = "color")
    private String color;


}
