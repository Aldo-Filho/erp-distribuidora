package com.pi.erp.warehouse.address;

import com.pi.erp.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouse_addresses")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WarehouseAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "warehouse_address_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

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

    public WarehouseAddress(RequestWarehouseAddressDTO requestWarehouseAddressDTO, Warehouse warehouse) {
        this.warehouse = warehouse;
        this.city = requestWarehouseAddressDTO.city();
        this.state = requestWarehouseAddressDTO.state();
        this.street = requestWarehouseAddressDTO.street();
        this.neighborhood = requestWarehouseAddressDTO.neighborhood();
        this.number = requestWarehouseAddressDTO.number();
        this.complement = requestWarehouseAddressDTO.complement();
        this.zipCode = requestWarehouseAddressDTO.zipCode();
    }
    
}
