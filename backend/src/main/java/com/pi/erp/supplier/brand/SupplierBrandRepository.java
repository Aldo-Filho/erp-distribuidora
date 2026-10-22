package com.pi.erp.supplier.brand;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierBrandRepository
    extends JpaRepository<SupplierBrand, SupplierBrandId>, JpaSpecificationExecutor<SupplierBrand>
{
    List<SupplierBrand> findAllBySupplierId(
        Long supplierId
    );

    void deleteAllBySupplierId(
        Long supplierId
    );
}
