package com.pi.erp.order;

import jakarta.validation.constraints.NotNull;

public record RequestOrderDTO(
    PaymentMethod paymentMethod,
    @NotNull Long customerId,
    Long userId,
    java.time.LocalDate paymentDueDate
) {
    public RequestOrderDTO(PaymentMethod paymentMethod, Long customerId, Long userId) {
        this(paymentMethod, customerId, userId, null);
    }
}
