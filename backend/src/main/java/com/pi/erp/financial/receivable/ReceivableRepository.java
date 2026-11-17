package com.pi.erp.financial.receivable;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ReceivableRepository extends JpaRepository<Receivable,Long>, JpaSpecificationExecutor<Receivable> {
    Optional<Receivable> findByOrderId(Long id);
    @Query(value="SELECT * FROM accounts_receivable WHERE account_receivable_id = :id FOR UPDATE", nativeQuery=true)
    Optional<Receivable> findByIdForUpdate(@Param("id") Long id);
}
