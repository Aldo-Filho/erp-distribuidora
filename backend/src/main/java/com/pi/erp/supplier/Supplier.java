package com.pi.erp.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.supplier.address.SupplierAddress;
import com.pi.erp.supplier.brand.SupplierBrand;
import com.pi.erp.supplier.category.SupplierCategory;
import com.pi.erp.supplier.product.ProductSupplier;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
    private List<SupplierAddress> supplierAddresses;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier")
    private List<ProductSupplier> productSuppliers;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier")
    private List<SupplierBrand> supplierBrands;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier")
    private List<SupplierCategory> supplierCategories;

    public Supplier(RequestSupplierDTO data) {
        this.legalName = data.legalName();
        this.tradeName = data.tradeName();
        this.taxId = data.taxId();
        this.email = data.email();
        this.phone = data.phone();
        this.whatsapp = data.whatsapp();
        this.avgDeliveryDays = data.avgDeliveryDays();
        this.active = data.active() == null || data.active();
    }
}
