package com.pi.erp.purchase;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository
    extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase>
{
    @Query(
        value = "SELECT * FROM purchases WHERE purchase_id = :id FOR UPDATE",
        nativeQuery = true
    )
    Optional<Purchase> findByIdForUpdate(
        @Param("id") Long id
    );
}
