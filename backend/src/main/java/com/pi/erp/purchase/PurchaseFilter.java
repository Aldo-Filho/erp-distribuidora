package com.pi.erp.purchase;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record PurchaseFilter(
    Long supplierId,
    Long userId,
    PurchaseStatus status,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo
) {}
