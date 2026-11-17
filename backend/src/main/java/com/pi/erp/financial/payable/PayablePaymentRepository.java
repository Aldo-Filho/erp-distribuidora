package com.pi.erp.financial.payable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PayablePaymentRepository extends JpaRepository<PayablePayment,Long> {
    List<PayablePayment> findByPayableIdOrderById(Long id);
    org.springframework.data.domain.Page<PayablePayment> findByPayableId(Long id, org.springframework.data.domain.Pageable pageable);
    boolean existsByPayableId(Long id);
}
