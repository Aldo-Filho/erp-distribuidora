package com.pi.erp.product.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestProductPresentationDTO(
    @NotNull Long productId,
    @NotBlank String name,
    @NotNull BigDecimal baseFactor,
    BigDecimal price,
    Boolean active
) {}
