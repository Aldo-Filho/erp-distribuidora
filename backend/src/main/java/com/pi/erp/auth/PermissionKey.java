package com.pi.erp.auth;

import java.util.Set;

public final class PermissionKey {

    private PermissionKey() {}

    public static final Set<String> ALL = Set.of(
        "customers.read",
        "customers.create",
        "customers.update",
        "customers.delete",
        "suppliers.read",
        "suppliers.create",
        "suppliers.update",
        "suppliers.delete",
        "products.read",
        "products.create",
        "products.update",
        "products.delete",
        "warehouses.read",
        "warehouses.create",
        "warehouses.update",
        "warehouses.delete",
        "stock.read",
        "stock.configure",
        "stock.adjust",
        "prices.read",
        "prices.create",
        "prices.update",
        "prices.delete",
        "orders.read",
        "orders.create",
        "orders.update",
        "orders.delete",
        "orders.confirm",
        "orders.cancel",
        "orders.dispatch",
        "orders.manualPrice",
        "orders.discount",
        "purchases.read",
        "purchases.create",
        "purchases.update",
        "purchases.delete",
        "purchases.send",
        "purchases.confirm",
        "purchases.cancel",
        "purchases.receive",
        "access.users.manage",
        "access.roles.manage",
        "security.settings.manage",
        "security.sessions.manage",
        "security.audit.read"
    );
}
