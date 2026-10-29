package com.pi.erp.order;

import jakarta.validation.constraints.NotNull;

public record RequestOrderDTO(
    PaymentMethod paymentMethod,
    @NotNull Long customerId,
    Long userId
) {}
