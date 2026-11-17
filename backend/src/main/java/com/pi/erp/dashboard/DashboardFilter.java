package com.pi.erp.dashboard;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
public record DashboardFilter(@DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                              @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to, Boolean compare, Long warehouseId,
                              String unit, String rankingMetric, Long productId) {
    public DashboardFilter {
        if (warehouseId != null && warehouseId <= 0 || productId != null && productId <= 0)
            throw new IllegalArgumentException("Identificador inválido.");
        if (unit != null && !java.util.Set.of("UN","KG","G","L","ML","M").contains(unit))
            throw new IllegalArgumentException("Unidade inválida.");
        if (rankingMetric != null && !java.util.Set.of("QUANTITY","REVENUE","AVERAGE_PRICE").contains(rankingMetric))
            throw new IllegalArgumentException("Métrica inválida.");
    }
    public String metric() { return rankingMetric == null ? "QUANTITY" : rankingMetric; }
}
