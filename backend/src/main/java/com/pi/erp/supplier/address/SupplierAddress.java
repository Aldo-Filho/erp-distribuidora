package com.pi.erp.supplier.address;

import com.pi.erp.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier_addresses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SupplierAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "supplier_address_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "country")
    private String country;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type")
    private AddressType addressType;

    public SupplierAddress(RequestSupplierAddressDTO data, Supplier supplier) {
        this.supplier = supplier;
        this.country = data.country();
        this.city = data.city();
        this.state = data.state();
        this.street = data.street();
        this.neighborhood = data.neighborhood();
        this.number = data.number();
        this.complement = data.complement();
        this.zipCode = data.zipCode();
        this.addressType = data.addressType();
    }
}
