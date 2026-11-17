package com.pi.erp.order;
import java.time.LocalDate;
public record ConfirmOrderDTO(PaymentMethod paymentMethod, LocalDate paymentDueDate) {}
