package com.pi.erp.financial.receivable;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="accounts_receivable_reversals") @Getter @Setter
public class ReceivableReversal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="reversal_id") private Long id;
    @Column(name="payment_id", nullable=false, updatable=false) private Long paymentId;
    @Column(name="user_id", nullable=false, updatable=false) private Long userId;
    @Column(name="reversed_at", nullable=false, updatable=false) private LocalDate reversedAt;
    @Column(nullable=false, updatable=false) private String reason;
    @Column(name="recorded_at", nullable=false, updatable=false) private LocalDateTime recordedAt;
    @PrePersist void created() { recordedAt = LocalDateTime.now(); }
}
