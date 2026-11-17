package com.pi.erp.financial.payable;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface PayableRepository extends JpaRepository<Payable,Long>, JpaSpecificationExecutor<Payable> {
    Optional<Payable> findByPurchaseId(Long id);
    @Query(value="SELECT * FROM accounts_payable WHERE account_payable_id = :id FOR UPDATE", nativeQuery=true)
    Optional<Payable> findByIdForUpdate(@Param("id") Long id);
}
