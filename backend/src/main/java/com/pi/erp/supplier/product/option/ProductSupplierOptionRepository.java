package com.pi.erp.supplier.product.option;

import java.math.BigDecimal;
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

public interface ProductSupplierOptionRepository
    extends
        JpaRepository<ProductSupplierOption, Long>,
        JpaSpecificationExecutor<ProductSupplierOption>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "productSupplier.product.catalog.brand",
            "productSupplier.product.catalog.category",
            "productSupplier.supplier",
        }
    )
    @Override
    Optional<ProductSupplierOption> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "productSupplier.product.catalog.brand",
            "productSupplier.product.catalog.category",
            "productSupplier.supplier",
        }
    )
    @Override
    List<ProductSupplierOption> findAll(Specification<ProductSupplierOption> spec);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "productSupplier.product.catalog.brand",
            "productSupplier.product.catalog.category",
            "productSupplier.supplier",
        }
    )
    @Override
    Page<ProductSupplierOption> findAll(
        Specification<ProductSupplierOption> spec,
        Pageable pageable
    );

    @Query(
        "SELECT o FROM ProductSupplierOption o WHERE o.productSupplier.id = :productSupplierId ORDER BY o.id"
    )
    List<ProductSupplierOption> findByProductSupplierIdOrderByIdAsc(
        @Param("productSupplierId") Long productSupplierId
    );

    @Query(
        "SELECT o FROM ProductSupplierOption o WHERE o.productSupplier.id = :productSupplierId AND o.active = TRUE AND o.conversionConfirmed = TRUE"
    )
    List<ProductSupplierOption> findByProductSupplierIdAndActiveTrueAndConversionConfirmedTrue(
        @Param("productSupplierId") Long productSupplierId
    );

    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM purchase_items WHERE supplier_option_id=:id)",
        nativeQuery = true
    )
    boolean hasUsage(@Param("id") Long id);

    @Query(
        value = """
            SELECT EXISTS(SELECT 1 FROM product_supplier_options WHERE product_supplier_id=:parentId
            AND lower(btrim(name))=lower(btrim(:name)) AND base_factor IS NOT DISTINCT FROM :factor
            AND product_supplier_option_id<>coalesce(:id,0))
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
