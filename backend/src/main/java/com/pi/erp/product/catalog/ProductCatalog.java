package com.pi.erp.product.catalog;

import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductUnit;
import com.pi.erp.product.brand.Brand;
import com.pi.erp.product.category.Category;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "product_catalogs")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_catalog_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    private ProductUnit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCatalogType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "product_catalog_characteristics",
        joinColumns = @JoinColumn(name = "product_catalog_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "characteristic")
    private Set<ProductCharacteristic> characteristics = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

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
}
