package com.pi.erp.financial;
import com.pi.erp.order.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@MappedSuperclass @Getter @Setter
public abstract class FinancialPayment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="payment_id") private Long id;
    @Column(name="bank_account_id", nullable=false, updatable=false) private Long bankAccountId;
    @Column(name="user_id", nullable=false, updatable=false) private Long userId;
    @Column(name="paid_amount", nullable=false, updatable=false, precision=12, scale=2) private BigDecimal paidAmount;
    @Column(name="paid_at", nullable=false, updatable=false) private LocalDate paidAt;
    @Enumerated(EnumType.STRING) @Column(name="payment_method", updatable=false) private PaymentMethod paymentMethod;
    @Column(updatable=false) private String notes;
    @Column(name="recorded_at", updatable=false) private LocalDateTime recordedAt;
    @Column(updatable=false) private boolean legacy;
    @PrePersist void created() { recordedAt = LocalDateTime.now(); }
}
