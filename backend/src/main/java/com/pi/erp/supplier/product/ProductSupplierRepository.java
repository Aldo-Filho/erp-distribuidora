package com.pi.erp.supplier.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductSupplierRepository
    extends JpaRepository<ProductSupplier, Long>, JpaSpecificationExecutor<ProductSupplier>
{
    Optional<ProductSupplier> findBySupplierIdAndProductId(
        Long supplierId,
        Long productId
    );

    boolean existsByProductIdAndSupplierId(
        Long productId,
        Long supplierId
    );

    boolean existsByProductIdAndSupplierIdAndIdNot(
        Long productId,
        Long supplierId,
        Long id
    );

    void deleteAllBySupplierId(
        Long supplierId
    );
}
