package com.pi.erp.product.brand;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(@NotBlank String name);
}
