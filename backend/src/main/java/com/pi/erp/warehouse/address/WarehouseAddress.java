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
    @JoinColumn(name = "warehouse_id", nullable = false, unique = true)
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

    public WarehouseAddress(
        RequestWarehouseAddressDTO data,
        Warehouse warehouse
    ) {
        this.warehouse = warehouse;
        this.state = data.state();
        this.city = data.city();
        this.street = data.street();
        this.neighborhood = data.neighborhood();
        this.number = data.number();
        this.complement = data.complement();
        this.zipCode = data.zipCode();
    }
}
