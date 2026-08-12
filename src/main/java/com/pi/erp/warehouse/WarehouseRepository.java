package com.pi.erp.warehouse;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByNameIgnoreCase(@NotBlank String name);

    List<Warehouse> findByNameContainingIgnoreCase(String name);
}
