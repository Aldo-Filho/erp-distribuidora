package com.pi.erp.financial;
import com.pi.erp.order.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record SettlementDTO(@NotNull Long bankAccountId,
    @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal amount,
    @NotNull @PastOrPresent LocalDate paidAt, @NotNull PaymentMethod paymentMethod,
    @Size(max=255) String notes) {}
