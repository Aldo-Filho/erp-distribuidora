package com.pi.erp.product.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository
    extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category>
{
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM categories
                WHERE lower(btrim(name)) = lower(btrim(cast(:name AS text)))
            )
            """,
        nativeQuery = true
    )
    boolean existsByNormalizedName(
        @Param("name") String name
    );
}
