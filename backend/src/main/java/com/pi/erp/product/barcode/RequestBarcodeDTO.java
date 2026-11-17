package com.pi.erp.product.barcode;
import jakarta.validation.constraints.*;
public record RequestBarcodeDTO(@NotNull Long productId,@NotBlank @Size(max=128) String code,
 @NotNull BarcodeType type,Boolean mainCode) {}
