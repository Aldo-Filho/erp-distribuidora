package com.pi.erp.product.presentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "product_presentations")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProductPresentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_presentation_id")
    @EqualsAndHashCode.Include
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_factor", nullable = false, precision = 14, scale = 1)
    private BigDecimal baseFactor;

    @Column(name = "base_presentation", nullable = false)
    private boolean basePresentation;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    public Long getProductId() {
        return product.getId();
    }

    public boolean isActive() {
        return basePresentation ? product.isActive() : active;
    }
}
