package com.pi.erp.financial;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record ReversalDTO(@NotNull @PastOrPresent LocalDate reversedAt,
    @NotBlank @Size(max=255) String reason) {}
