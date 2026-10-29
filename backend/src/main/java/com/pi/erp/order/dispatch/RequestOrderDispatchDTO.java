package com.pi.erp.order.dispatch;

public record RequestOrderDispatchDTO(
    Long operatorUserId,
    String notes
) {}
