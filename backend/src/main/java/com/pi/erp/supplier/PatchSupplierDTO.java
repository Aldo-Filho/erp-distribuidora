package com.pi.erp.supplier;

public record PatchSupplierDTO(
    String legalName,
    String tradeName,
    String taxId,
    String email,
    String phone,
    String whatsapp,
    Integer avgDeliveryDays,
    Boolean active
) {}
