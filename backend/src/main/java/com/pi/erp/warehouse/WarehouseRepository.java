package com.pi.erp.warehouse;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByNameIgnoreCase(@NotBlank String name);

    List<Warehouse> findByNameContainingIgnoreCase(String name);
}
