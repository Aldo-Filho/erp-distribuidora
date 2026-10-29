package com.pi.erp.purchase;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RequestPurchaseDTO(
    @NotNull Long supplierId,
    @NotNull Long userId,
    LocalDate expectedDeliveryDate
) {}
