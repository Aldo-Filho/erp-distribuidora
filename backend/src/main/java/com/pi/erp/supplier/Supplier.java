package com.pi.erp.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.supplier.address.SupplierAddress;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Supplier {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long id;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "tax_id", unique = true)
    private String taxId;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "avg_delivery_days")
    private Integer avgDeliveryDays;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnore
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierAddress> supplierAddress;

    public Supplier(RequestSupplierDTO requestSupplierDTO) {
        this.legalName = requestSupplierDTO.legalName();
        this.tradeName = requestSupplierDTO.tradeName();
        this.taxId = requestSupplierDTO.taxId();
        this.email = requestSupplierDTO.email();
        this.phone = requestSupplierDTO.phone();
        this.whatsapp = requestSupplierDTO.whatsapp();
        this.active = true;
    }
}
