package com.pi.erp.order;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestOrderDTO(
    BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    @NotNull OrderStatus status,
    @NotNull Long customerId,
    Long userId
) {}
