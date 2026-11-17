package com.pi.erp.financial.payable;
import com.pi.erp.financial.FinancialPayment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="accounts_payable_payments") @Getter @Setter
public class PayablePayment extends FinancialPayment {
    @Column(name="account_payable_id", nullable=false, updatable=false) private Long payableId;
}
