package com.pi.erp.supplier.brand;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SupplierBrandId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;
}
