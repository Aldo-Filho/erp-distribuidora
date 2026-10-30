package com.pi.erp.product.presentation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPresentationRepository
    extends JpaRepository<ProductPresentation, Long>, JpaSpecificationExecutor<ProductPresentation>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "product.catalog.brand", "product.catalog.category" }
    )
    @Override
    Optional<ProductPresentation> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "product.catalog.brand", "product.catalog.category" }
    )
    @Override
    List<ProductPresentation> findAll(Specification<ProductPresentation> spec);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = { "product.catalog.brand", "product.catalog.category" }
    )
    @Override
    Page<ProductPresentation> findAll(Specification<ProductPresentation> spec, Pageable pageable);

    @Query(
        "SELECT p FROM ProductPresentation p WHERE p.product.id = :productId AND p.basePresentation = TRUE"
    )
    Optional<ProductPresentation> findByProductIdAndBasePresentationTrue(
        @Param("productId") Long productId
    );

    @Modifying
    @Query(
        value = """
            DELETE FROM product_presentations p WHERE p.product_id=:id
            AND NOT EXISTS(SELECT 1 FROM order_items i WHERE i.presentation_id=p.product_presentation_id)
            AND NOT EXISTS(SELECT 1 FROM price_table_items i WHERE i.presentation_id=p.product_presentation_id)
        """,
        nativeQuery = true
    )
    void deleteUnusedByProductId(@Param("id") Long id);

    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM order_items WHERE presentation_id=:id)",
        nativeQuery = true
    )
    boolean hasUsage(@Param("id") Long id);

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM product_presentations WHERE product_id=:parentId
            AND lower(btrim(name))=lower(btrim(:name)) AND base_factor IS NOT DISTINCT FROM :factor
            AND product_presentation_id<>coalesce(:id,0))
        """,
        nativeQuery = true
    )
    boolean existsByIdentity(
        @Param("parentId") Long parentId,
        @Param("name") String name,
        @Param("factor") BigDecimal factor,
        @Param("id") Long id
    );
}
