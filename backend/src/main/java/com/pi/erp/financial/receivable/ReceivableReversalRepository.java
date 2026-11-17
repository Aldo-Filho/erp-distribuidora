package com.pi.erp.financial.receivable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceivableReversalRepository extends JpaRepository<ReceivableReversal,Long> {
    org.springframework.data.domain.Page<ReceivableReversal> findByPaymentId(Long paymentId, org.springframework.data.domain.Pageable pageable);
    Optional<ReceivableReversal> findByPaymentId(Long paymentId);
}
