package com.pi.erp.warehouse;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.erp.warehouse.address.WarehouseAddress;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouses")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "warehouse_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @JsonIgnore
    @OneToOne(
            mappedBy = "warehouse",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private WarehouseAddress warehouseAddresses;

    public Warehouse (RequestWarehouseDTO requestWarehouseDTO) {
        this.name = requestWarehouseDTO.name();
        this.description = requestWarehouseDTO.description();
    }
}
