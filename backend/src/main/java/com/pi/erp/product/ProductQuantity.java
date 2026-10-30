package com.pi.erp.product;

import java.math.BigDecimal;

public final class ProductQuantity {

    private static final BigDecimal MAX_QUANTITY = new BigDecimal("9999999999999.9");

    private ProductQuantity() {}

    public static BigDecimal requireBase(BigDecimal value, ProductUnit unit, boolean allowZero) {
        requireUnit(unit);
        requireRange(value);
        if (value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            throw new IllegalArgumentException("Quantity must be positive or an allowed zero.");
        }
        requireScale(value, unit == ProductUnit.UN ? 0 : 1);
        return value.setScale(1);
    }

    public static BigDecimal requireSignedBase(BigDecimal value, ProductUnit unit) {
        requireUnit(unit);
        requireRange(value);
        if (value.signum() == 0) {
            throw new IllegalArgumentException("Adjustment quantity must not be zero.");
        }
        requireScale(value, unit == ProductUnit.UN ? 0 : 1);
        return value.setScale(1);
    }

    public static BigDecimal toBase(
        BigDecimal quantity,
        BigDecimal factor,
        ProductUnit unit,
        boolean basePresentation
    ) {
        requireBase(factor, unit, false);
        if (basePresentation && factor.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Base presentation factor must be one.");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Commercial quantity must be positive.");
        }
        requireRange(quantity);
        requireScale(quantity, basePresentation && unit != ProductUnit.UN ? 1 : 0);
        return requireBase(quantity.multiply(factor), unit, false);
    }

    private static void requireUnit(ProductUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException(
                "Confirm the legacy base unit before this operation."
            );
        }
    }

    private static void requireRange(BigDecimal value) {
        if (value == null || value.abs().compareTo(MAX_QUANTITY) > 0) {
            throw new IllegalArgumentException(
                "Quantity is required and must not exceed 9999999999999.9."
            );
        }
    }

    private static void requireScale(BigDecimal value, int scale) {
        if (value.stripTrailingZeros().scale() > scale) {
            throw new IllegalArgumentException(
                "Quantity precision is not supported by the base unit."
            );
        }
    }
}
