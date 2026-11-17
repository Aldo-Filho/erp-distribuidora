package com.pi.erp.financial.receivable;
import com.pi.erp.financial.FinancialTitle;
import com.pi.erp.order.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="accounts_receivable") @Getter @Setter
public class Receivable extends FinancialTitle {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="account_receivable_id") private Long id;
    @Column(name="customer_id", updatable=false) private Long customerId;
    @Column(name="order_id", updatable=false) private Long orderId;
    @Enumerated(EnumType.STRING) @Column(name="payment_method", updatable=false) private PaymentMethod paymentMethod;
}
