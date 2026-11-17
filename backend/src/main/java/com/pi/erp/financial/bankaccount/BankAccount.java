package com.pi.erp.financial.bankaccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="bank_accounts") @Getter @Setter
public class BankAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="bank_account_id") private Long id;
    @Column(nullable=false,length=100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private BankAccountType type;
    @Column(name="initial_balance",nullable=false,precision=12,scale=2) private BigDecimal initialBalance;
    private boolean active = true;
    @Column(name="updated_at") private java.time.LocalDateTime updatedAt;
    @Column(name="updated_by") private Long updatedBy;
    @PrePersist @PreUpdate void audit() {
        updatedAt = java.time.LocalDateTime.now();
        updatedBy = com.pi.erp.auth.CurrentUser.requireId();
    }
}
