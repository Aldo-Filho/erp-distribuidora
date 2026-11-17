package com.pi.erp.financial.receivable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceivablePaymentRepository extends JpaRepository<ReceivablePayment,Long> {
    List<ReceivablePayment> findByReceivableIdOrderById(Long id);
    org.springframework.data.domain.Page<ReceivablePayment> findByReceivableId(Long id, org.springframework.data.domain.Pageable pageable);
    boolean existsByReceivableId(Long id);
}
