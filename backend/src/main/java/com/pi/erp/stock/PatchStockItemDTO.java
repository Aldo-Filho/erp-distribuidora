package com.pi.erp.stock;

import java.math.BigDecimal;

public record PatchStockItemDTO(
    Long warehouseId,
    Long productId,
    BigDecimal quantity,
    BigDecimal reservedQuantity,
    BigDecimal minQuantity,
    BigDecimal maxQuantity,
    Boolean removeMaxQuantity
) {}
