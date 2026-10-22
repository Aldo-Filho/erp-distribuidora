package com.pi.erp.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.customer.address.CustomerAddress;
import com.pi.erp.price.PriceTable;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(
    name = "customers",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_tax_id", columnNames = { "tax_id" })
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "customer_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 2)
    private PersonType personType;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "municipal_reg")
    private String municipalReg;

    @Column(name = "state_reg")
    private String stateReg;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "created_at")
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

    @ManyToOne
    @JoinColumn(name = "price_table_id")
    private PriceTable priceTable;

    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddress> customerAddresses;

    public Customer(RequestCustomerDTO data, PriceTable priceTable) {
        this.personType = data.personType();
        this.taxId = data.taxId();
        this.legalName = data.legalName();
        this.tradeName = data.tradeName();
        this.birthDate = data.birthDate();
        this.municipalReg = data.municipalReg();
        this.stateReg = data.stateReg();
        this.email = data.email();
        this.phone = data.phone();
        this.whatsapp = data.whatsapp();
        this.active = data.active() == null || data.active();
        this.priceTable = priceTable;
    }
}
