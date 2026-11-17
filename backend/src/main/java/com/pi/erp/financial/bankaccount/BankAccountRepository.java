package com.pi.erp.financial.bankaccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface BankAccountRepository extends JpaRepository<BankAccount,Long>, JpaSpecificationExecutor<BankAccount> {
    @Query(value="SELECT * FROM bank_accounts WHERE bank_account_id = :id FOR UPDATE",nativeQuery=true)
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
