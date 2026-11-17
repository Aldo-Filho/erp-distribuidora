package com.pi.erp.financial.cashflow;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
@Entity @Immutable @Table(name="financial_cash_flow") @Getter
public class CashFlowEntry {
    @Id private String id;
    private String kind;
    private String direction;
    @Column(name="title_id") private Long titleId;
    @Column(name="document_id") private Long documentId;
    @Column(name="party_id") private Long partyId;
    @Column(name="category_id") private Long categoryId;
    @Column(name="bank_account_id") private Long bankAccountId;
    @Column(name="occurred_on") private LocalDate occurredOn;
    private BigDecimal amount;
    @Column(name="payment_id") private Long paymentId;
    @Column(name="reversal_id") private Long reversalId;
}
