package com.pi.erp.purchase.receipt;

import com.pi.erp.purchase.Purchase;
import com.pi.erp.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "purchase_receipts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_purchase_receipt_request",
        columnNames = { "purchase_id", "request_id" }
    )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PurchaseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "purchase_receipt_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @ManyToOne
    @JoinColumn(name = "operator_user_id", nullable = false)
    private User operator;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "receipt")
    private List<PurchaseReceiptItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        receivedAt = LocalDateTime.now();
    }

    public PurchaseReceipt(Purchase purchase, UUID requestId, User operator, String notes) {
        this.purchase = purchase;
        this.requestId = requestId;
        this.operator = operator;
        this.notes = notes;
    }
}
