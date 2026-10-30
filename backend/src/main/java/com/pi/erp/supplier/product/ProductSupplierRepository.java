package com.pi.erp.supplier.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSupplierRepository
    extends JpaRepository<ProductSupplier, Long>, JpaSpecificationExecutor<ProductSupplier>
{
    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "supplier",
            "legacyOption",
        }
    )
    @Override
    Optional<ProductSupplier> findById(Long id);

    @EntityGraph(
        type = EntityGraph.EntityGraphType.LOAD,
        attributePaths = {
            "product.catalog.brand",
            "product.catalog.category",
            "supplier",
            "legacyOption",
        }
    )
    @Override
    List<ProductSupplier> findAll(Specification<ProductSupplier> spec);

    Optional<ProductSupplier> findBySupplierIdAndProductId(Long supplierId, Long productId);

    boolean existsByProductIdAndSupplierId(Long productId, Long supplierId);

    boolean existsByProductIdAndSupplierIdAndIdNot(Long productId, Long supplierId, Long id);

    List<ProductSupplier> findByProductIdOrderByIdAsc(Long productId);

    List<ProductSupplier> findBySupplierIdOrderByIdAsc(Long supplierId);

    void deleteAllBySupplierId(Long supplierId);

    @Query(
        value = "SELECT * FROM product_suppliers WHERE product_supplier_id=:id FOR UPDATE",
        nativeQuery = true
    )
    Optional<ProductSupplier> findByIdForUpdate(@Param("id") Long id);

    @Query(
        value = "SELECT * FROM product_suppliers WHERE product_id=:id ORDER BY product_supplier_id FOR UPDATE",
        nativeQuery = true
    )
    List<ProductSupplier> findByProductIdForUpdate(@Param("id") Long id);
}
