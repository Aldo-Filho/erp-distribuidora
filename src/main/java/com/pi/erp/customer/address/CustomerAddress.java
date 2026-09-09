package com.pi.erp.customer.address;

import com.pi.erp.customer.Customer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "customer_address_id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type")
    private AddressType addressType;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "state")
    private String state;

    @Column(name = "city")
    private String city;

    @Column(name = "street")
    private String street;

    @Column(name = "neighborhood")
    private String neighborhood;

    @Column(name = "number")
    private String number;

    @Column(name = "complement")
    private String complement;

    @Column(name = "zip_code")
    private String zipCode;

}
