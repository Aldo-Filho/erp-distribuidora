package com.pi.erp.financial.bankaccount;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record RequestBankAccountDTO(@NotBlank @Size(max=100) String name, @NotNull BankAccountType type,
    @NotNull @Digits(integer=10,fraction=2) BigDecimal initialBalance) {}
