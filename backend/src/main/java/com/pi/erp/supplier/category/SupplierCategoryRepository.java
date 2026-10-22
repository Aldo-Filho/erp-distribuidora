package com.pi.erp.supplier.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierCategoryRepository
    extends JpaRepository<SupplierCategory, SupplierCategoryId>, JpaSpecificationExecutor<SupplierCategory>
{
    List<SupplierCategory> findAllBySupplierId(
        Long supplierId
    );

    void deleteAllBySupplierId(
        Long supplierId
    );
}
