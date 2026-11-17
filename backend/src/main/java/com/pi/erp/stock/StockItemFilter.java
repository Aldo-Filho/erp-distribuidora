package com.pi.erp.stock;

public record StockItemFilter(
    Long warehouseId,
    Long productId,
    Boolean lowStock
) {
    public StockItemFilter(Long warehouseId, Long productId) { this(warehouseId,productId,null); }
}
