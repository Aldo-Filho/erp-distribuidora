package com.pi.erp.price.item;

import java.math.BigDecimal;

public record PatchPriceTableItemDTO(
    Long priceTableId,
    Long productId,
    BigDecimal price,
    Long presentationId,
    Boolean removePresentation
) {}
