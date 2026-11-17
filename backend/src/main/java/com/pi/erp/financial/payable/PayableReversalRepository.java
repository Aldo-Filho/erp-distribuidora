package com.pi.erp.financial.payable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PayableReversalRepository extends JpaRepository<PayableReversal,Long> {
    org.springframework.data.domain.Page<PayableReversal> findByPaymentId(Long paymentId, org.springframework.data.domain.Pageable pageable);
    Optional<PayableReversal> findByPaymentId(Long paymentId);
}
