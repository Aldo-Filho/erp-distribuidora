package com.pi.erp.product.category;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByNameContainingIgnoreCase(@NotBlank String name);

    boolean existsByNameIgnoreCase(String name);

}
