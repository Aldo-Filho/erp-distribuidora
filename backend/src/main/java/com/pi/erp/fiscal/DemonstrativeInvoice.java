package com.pi.erp.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DemonstrativeInvoice(Long id, String number, Long orderId, OffsetDateTime issuedAt,
                                   String notice, Snapshot snapshot) {
    public static final String NOTICE = "DEMONSTRATIVO — SEM VALIDADE FISCAL";
    public record Address(String type, String country, String state, String city, String street,
                          String neighborhood, String number, String complement, String zipCode) {}
    public record Party(String legalName, String tradeName, String taxId, String stateRegistration,
                        List<Address> addresses) {}
    public record Item(Long orderItemId, Long productId, String sku, String name, Map<String,String> attributes, String presentation,
                       String baseUnit, BigDecimal quantity, BigDecimal baseFactor, BigDecimal baseQuantity,
                       BigDecimal unitPrice, String discountType, BigDecimal discount,
                       BigDecimal grossAmount, BigDecimal discountAmount, BigDecimal totalAmount) {}
    public record Snapshot(int schemaVersion, String provider, String notice, Party issuer, Party customer,
                           String paymentMethod, LocalDate paymentDueDate, List<Item> items,
                           BigDecimal productsAmount, BigDecimal discountAmount, BigDecimal totalAmount,
                           List<String> warnings) {}
}
