package com.pi.erp.financial.bankaccount;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PatchBankAccountDTO(@Size(min=1,max=100) String name, BankAccountType type,
    @Digits(integer=10,fraction=2) BigDecimal initialBalance, Boolean active) {}
