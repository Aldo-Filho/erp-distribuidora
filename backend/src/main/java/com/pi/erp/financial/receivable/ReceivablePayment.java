package com.pi.erp.financial.receivable;
import com.pi.erp.financial.FinancialPayment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="accounts_receivable_payments") @Getter @Setter
public class ReceivablePayment extends FinancialPayment {
    @Column(name="account_receivable_id", nullable=false, updatable=false) private Long receivableId;
}
