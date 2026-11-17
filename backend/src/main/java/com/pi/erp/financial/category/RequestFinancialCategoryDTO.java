package com.pi.erp.financial.category;
import jakarta.validation.constraints.*;
public record RequestFinancialCategoryDTO(@NotBlank @Size(max=100) String name,
    @NotNull CategoryType type, Boolean defaultForDocuments) {}
