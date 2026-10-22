package com.pi.erp.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>
{
    @Query(
        value = "SELECT nextval('product_sku_seq')",
        nativeQuery = true
    )
    Long nextSkuNumber();
}
