package com.pi.erp.product.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCatalogRepository
    extends JpaRepository<ProductCatalog, Long>, JpaSpecificationExecutor<ProductCatalog>
{
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = { "brand", "category" })
    @Override
    Optional<ProductCatalog> findById(Long id);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = { "brand", "category" })
    @Override
    List<ProductCatalog> findAll(Specification<ProductCatalog> spec);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = { "brand", "category" })
    @Override
    Page<ProductCatalog> findAll(Specification<ProductCatalog> spec, Pageable pageable);

    @Query(
        value = "SELECT * FROM product_catalogs WHERE product_catalog_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<ProductCatalog> findByIdForUpdate(@Param("id") Long id);

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM products p WHERE p.catalog_id=:id AND (
            EXISTS(SELECT 1 FROM stock_items s WHERE s.product_id=p.product_id)
            OR EXISTS(SELECT 1 FROM order_items o WHERE o.product_id=p.product_id)
            OR EXISTS(SELECT 1 FROM purchase_items i WHERE i.product_id=p.product_id)
            OR EXISTS(SELECT 1 FROM price_table_items i WHERE i.product_id=p.product_id)
            OR EXISTS(SELECT 1 FROM product_presentations x WHERE x.product_id=p.product_id AND NOT x.base_presentation)
            OR EXISTS(SELECT 1 FROM product_supplier_options x JOIN product_suppliers s USING(product_supplier_id)
                WHERE s.product_id=p.product_id AND x.conversion_confirmed)))
        """,
        nativeQuery = true
    )
    boolean hasUnitUsage(@Param("id") Long id);
}
