package com.pi.erp.price;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RequestPriceTableDTO(
        @NotBlank
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active
) {
}
