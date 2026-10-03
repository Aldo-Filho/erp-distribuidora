package com.pi.erp.price;

import java.time.LocalDate;

public record PriceTableFilter(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active
) {
}
