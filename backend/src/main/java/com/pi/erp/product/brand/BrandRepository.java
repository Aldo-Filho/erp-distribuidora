package com.pi.erp.product.brand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandRepository
    extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand>
{
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM brands
                WHERE lower(btrim(name)) = lower(btrim(cast(:name AS text)))
            )
            """,
        nativeQuery = true
    )
    boolean existsByNormalizedName(
        @Param("name") String name
    );
}
