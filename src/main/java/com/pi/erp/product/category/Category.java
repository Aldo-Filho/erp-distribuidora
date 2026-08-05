package com.pi.erp.product.category;

import com.pi.erp.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "category")
    private List<Product> products;
}
