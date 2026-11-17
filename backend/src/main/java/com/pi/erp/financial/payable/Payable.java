package com.pi.erp.financial.payable;
import com.pi.erp.financial.FinancialTitle;
import com.pi.erp.order.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="accounts_payable") @Getter @Setter
public class Payable extends FinancialTitle {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="account_payable_id") private Long id;
    @Column(name="supplier_id", updatable=false) private Long supplierId;
    @Column(name="purchase_id", updatable=false) private Long purchaseId;
    @Enumerated(EnumType.STRING) @Column(name="payment_method") private PaymentMethod paymentMethod;
}
