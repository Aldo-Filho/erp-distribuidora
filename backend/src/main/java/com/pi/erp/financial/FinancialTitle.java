package com.pi.erp.financial;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@MappedSuperclass @Getter @Setter
public abstract class FinancialTitle {
    @Column(name="financial_category_id", nullable=false) private Long categoryId;
    private String description;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(name="due_date", nullable=false) private LocalDate dueDate;
    @Enumerated(EnumType.STRING) private FinancialStatus status;
    @Column(name="issued_at", nullable=false, updatable=false) private LocalDateTime issuedAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="created_by", updatable=false) private Long createdBy;
    @Column(name="updated_by") private Long updatedBy;
    @Column(nullable=false, updatable=false) private boolean legacy;
    @PrePersist void created() { issuedAt = LocalDateTime.now(); }
    @PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
