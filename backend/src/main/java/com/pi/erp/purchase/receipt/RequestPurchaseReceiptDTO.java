package com.pi.erp.purchase.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RequestPurchaseReceiptDTO(
    @NotNull UUID requestId,
    Long operatorUserId,
    String notes,
    @NotEmpty @Valid List<ItemDTO> items
) {
    public record ItemDTO(@NotNull Long purchaseItemId, @NotNull BigDecimal quantity) {}
}
