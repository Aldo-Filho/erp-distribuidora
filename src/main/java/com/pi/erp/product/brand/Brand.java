package com.pi.erp.product.brand;

import com.pi.erp.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "brand")
    private List<Product> products;
}
