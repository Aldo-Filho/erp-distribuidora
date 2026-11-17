package com.pi.erp.financial.category;
import jakarta.validation.constraints.Size;
public record PatchFinancialCategoryDTO(@Size(min=1,max=100) String name,
    Boolean active, Boolean defaultForDocuments) {}
