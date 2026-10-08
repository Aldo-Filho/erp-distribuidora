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
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public SupplierAddress(RequestSupplierAddressDTO requestSupplierAddressDTO, Supplier supplier) {
        this.supplier = supplier;
        this.country = requestSupplierAddressDTO.country();
        this.city = requestSupplierAddressDTO.city();
        this.state = requestSupplierAddressDTO.state();
        this.street = requestSupplierAddressDTO.street();
        this.neighborhood = requestSupplierAddressDTO.neighborhood();
        this.number = requestSupplierAddressDTO.number();
        this.complement = requestSupplierAddressDTO.complement();
        this.zipCode = requestSupplierAddressDTO.zipCode();
        this.addressType = requestSupplierAddressDTO.addressType();
    }
}
