package com.pi.erp.product;

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

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "catalog.brand", "catalog.category" }
    )
    @Override
    Optional<Product> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "catalog.brand", "catalog.category" }
    )
    @Override
    List<Product> findAll(Specification<Product> spec);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "catalog.brand", "catalog.category" }
    )
    @Override
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "catalog.brand", "catalog.category" }
    )
    @Query("SELECT p FROM Product p WHERE p.catalog.id = :catalogId ORDER BY p.id")
    List<Product> findByCatalogIdOrderByIdAsc(@Param("catalogId") Long catalogId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.catalog.id = :catalogId")
    long countByCatalogId(@Param("catalogId") Long catalogId);

    @Query(
        "SELECT COUNT(p) > 0 FROM Product p WHERE p.catalog.id = :catalogId AND p.variantKey = :variantKey"
    )
    boolean existsByCatalogIdAndVariantKey(
        @Param("catalogId") Long catalogId,
        @Param("variantKey") String variantKey
    );

    @Query(
        "SELECT COUNT(p) > 0 FROM Product p WHERE p.catalog.id = :catalogId AND p.variantKey = :variantKey AND p.id <> :id"
    )
    boolean existsByCatalogIdAndVariantKeyAndIdNot(
        @Param("catalogId") Long catalogId,
        @Param("variantKey") String variantKey,
        @Param("id") Long id
    );

    @Query(value = "SELECT * FROM products WHERE product_id = :id FOR UPDATE", nativeQuery = true)
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT catalog_id FROM products WHERE product_id = :id", nativeQuery = true)
    Optional<Long> findCatalogId(@Param("id") Long id);

    @Query(value = "SELECT nextval('product_sku_seq')", nativeQuery = true)
    Long nextSkuNumber();
}
