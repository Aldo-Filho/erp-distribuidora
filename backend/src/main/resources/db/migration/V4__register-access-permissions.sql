-- Technical catalog only. Existing fixture roles/users are never promoted.
INSERT INTO permissions(module, action, permission_key) VALUES
('customers', 'read', 'customers.read'),
('customers', 'create', 'customers.create'),
('customers', 'update', 'customers.update'),
('customers', 'delete', 'customers.delete'),
('suppliers', 'read', 'suppliers.read'),
('suppliers', 'create', 'suppliers.create'),
('suppliers', 'update', 'suppliers.update'),
('suppliers', 'delete', 'suppliers.delete'),
('products', 'read', 'products.read'),
('products', 'create', 'products.create'),
('products', 'update', 'products.update'),
('products', 'delete', 'products.delete'),
('warehouses', 'read', 'warehouses.read'),
('warehouses', 'create', 'warehouses.create'),
('warehouses', 'update', 'warehouses.update'),
('warehouses', 'delete', 'warehouses.delete'),
('stock', 'read', 'stock.read'),
('stock', 'configure', 'stock.configure'),
('stock', 'adjust', 'stock.adjust'),
('prices', 'read', 'prices.read'),
('prices', 'create', 'prices.create'),
('prices', 'update', 'prices.update'),
('prices', 'delete', 'prices.delete'),
('orders', 'read', 'orders.read'),
('orders', 'create', 'orders.create'),
('orders', 'update', 'orders.update'),
('orders', 'delete', 'orders.delete'),
('orders', 'confirm', 'orders.confirm'),
('orders', 'cancel', 'orders.cancel'),
('orders', 'dispatch', 'orders.dispatch'),
('orders', 'manualPrice', 'orders.manualPrice'),
('orders', 'discount', 'orders.discount'),
('purchases', 'read', 'purchases.read'),
('purchases', 'create', 'purchases.create'),
('purchases', 'update', 'purchases.update'),
('purchases', 'delete', 'purchases.delete'),
('purchases', 'send', 'purchases.send'),
('purchases', 'confirm', 'purchases.confirm'),
('purchases', 'cancel', 'purchases.cancel'),
('purchases', 'receive', 'purchases.receive'),
('access.users', 'manage', 'access.users.manage'),
('access.roles', 'manage', 'access.roles.manage'),
('security.settings', 'manage', 'security.settings.manage'),
('security.sessions', 'manage', 'security.sessions.manage'),
('security.audit', 'read', 'security.audit.read')
ON CONFLICT(permission_key) DO NOTHING;

-- Insert new roles independently of any similarly named development fixtures.
DO $$
DECLARE administrator BIGINT; sales BIGINT; purchasing BIGINT; inventory BIGINT;
BEGIN
    INSERT INTO roles(name, description) VALUES ('Administrador', 'Initial configurable access template') RETURNING role_id INTO administrator;
    INSERT INTO roles(name, description) VALUES ('Vendas', 'Initial configurable access template') RETURNING role_id INTO sales;
    INSERT INTO roles(name, description) VALUES ('Compras', 'Initial configurable access template') RETURNING role_id INTO purchasing;
    INSERT INTO roles(name, description) VALUES ('Estoque', 'Initial configurable access template') RETURNING role_id INTO inventory;
    INSERT INTO roles(name, description) VALUES ('Financeiro', 'Initial configurable access template');
    INSERT INTO role_permissions SELECT administrator, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'products.create', 'products.update', 'products.delete', 'warehouses.read', 'warehouses.create', 'warehouses.update', 'warehouses.delete', 'stock.read', 'stock.configure', 'stock.adjust', 'prices.read', 'prices.create', 'prices.update', 'prices.delete', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.dispatch', 'orders.manualPrice', 'orders.discount', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel', 'purchases.receive', 'access.users.manage', 'access.roles.manage', 'security.settings.manage', 'security.sessions.manage', 'security.audit.read');
    INSERT INTO role_permissions SELECT sales, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'products.read', 'stock.read', 'prices.read', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.manualPrice', 'orders.discount');
    INSERT INTO role_permissions SELECT purchasing, permission_id FROM permissions WHERE permission_key IN ('suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'stock.read', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel');
    INSERT INTO role_permissions SELECT inventory, permission_id FROM permissions WHERE permission_key IN ('products.read', 'warehouses.read', 'stock.read', 'stock.adjust', 'orders.read', 'orders.dispatch', 'purchases.read', 'purchases.receive');
END $$;

