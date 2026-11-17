package com.pi.erp.financial.category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="financial_categories") @Getter @Setter
public class FinancialCategory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="financial_category_id") private Long id;
    @Column(nullable=false,length=100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private CategoryType type;
    private boolean active = true;
    @Column(name="default_for_documents") private boolean defaultForDocuments;
    @Column(name="updated_at") private java.time.LocalDateTime updatedAt;
    @Column(name="updated_by") private Long updatedBy;
    @PrePersist @PreUpdate void audit() {
        updatedAt = java.time.LocalDateTime.now();
        updatedBy = com.pi.erp.auth.CurrentUser.requireId();
    }
}
