package com.pi.erp.customer.address;

import com.pi.erp.customer.Customer;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")

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
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type")
    private AddressType addressType;

    @ManyToOne
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

    public CustomerAddress(RequestCustomerAddressDTO data, Customer customer) {
        this.customer = customer;
        this.addressType = data.addressType();
        this.state = data.state();
        this.city = data.city();
        this.street = data.street();
        this.neighborhood = data.neighborhood();
        this.number = data.number();
        this.complement = data.complement();
        this.zipCode = data.zipCode();
    }

}
