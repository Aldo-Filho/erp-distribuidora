package com.pi.erp.stock;

public record PatchStockItemDTO(
        Long warehouseId,
        Long productId,
        Integer quantity,
        Integer reservedQuantity,
        Integer minQuantity,
        Integer maxQuantity
) {
}
