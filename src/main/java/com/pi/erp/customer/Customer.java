package com.pi.erp.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.customer.address.CustomerAddress;
import com.pi.erp.price.PriceTable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_tax_id",
                columnNames = {"tax_id"}
        )
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
    private boolean active = true;

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
    @OneToOne(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private CustomerAddress customerAddress;

    public Customer(RequestCustomerDTO requestCustomerDTO, PriceTable priceTable) {
        this.personType = requestCustomerDTO.personType();
        this.taxId = requestCustomerDTO.taxId();
        this.legalName = requestCustomerDTO.legalName();
        this.tradeName = requestCustomerDTO.tradeName();
        this.birthDate = requestCustomerDTO.birthDate();
        this.municipalReg = requestCustomerDTO.municipalReg();
        this.stateReg = requestCustomerDTO.stateReg();
        this.email = requestCustomerDTO.email();
        this.phone = requestCustomerDTO.phone();
        this.whatsapp = requestCustomerDTO.whatsapp();
        this.active = requestCustomerDTO.active() != null
                ? requestCustomerDTO.active()
                : true;
        this.priceTable = priceTable;
    }

}
