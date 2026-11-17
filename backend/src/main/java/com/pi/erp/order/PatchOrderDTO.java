package com.pi.erp.order;

public record PatchOrderDTO(
    PaymentMethod paymentMethod,
    OrderStatus status,
    Long customerId,
    Long userId,
    java.time.LocalDate paymentDueDate
) {
    public PatchOrderDTO(PaymentMethod paymentMethod, OrderStatus status, Long customerId, Long userId) {
        this(paymentMethod, status, customerId, userId, null);
    }
}
