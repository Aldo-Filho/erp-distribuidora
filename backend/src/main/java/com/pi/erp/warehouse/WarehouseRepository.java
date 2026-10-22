package com.pi.erp.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository
    extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse>
{
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM warehouses
                WHERE lower(btrim(name)) = lower(btrim(cast(:name AS text)))
            )
            """,
        nativeQuery = true
    )
    boolean existsByNormalizedName(
        @Param("name") String name
    );
}
