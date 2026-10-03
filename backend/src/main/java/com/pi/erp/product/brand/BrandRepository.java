package com.pi.erp.product.brand;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(@NotBlank String name);
}
