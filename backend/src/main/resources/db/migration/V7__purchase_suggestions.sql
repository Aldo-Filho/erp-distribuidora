ALTER TABLE purchase_suggestions ADD COLUMN request_id UUID UNIQUE;
ALTER TABLE purchase_suggestions ADD COLUMN generated_by BIGINT REFERENCES users(user_id);
ALTER TABLE purchase_suggestions ADD COLUMN period_from TIMESTAMP;
ALTER TABLE purchase_suggestions ADD COLUMN period_to TIMESTAMP;
ALTER TABLE purchase_suggestion_items ALTER COLUMN supplier_id DROP NOT NULL;
ALTER TABLE purchase_suggestion_items ALTER COLUMN period_sold_quantity TYPE NUMERIC(20,1);
ALTER TABLE purchase_suggestion_items ALTER COLUMN avg_daily_consumption TYPE NUMERIC(24,10);
ALTER TABLE purchase_suggestion_items ALTER COLUMN current_stock TYPE NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ALTER COLUMN min_stock TYPE NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ALTER COLUMN suggested_quantity TYPE NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN warehouse_id BIGINT REFERENCES warehouses(warehouse_id);
ALTER TABLE purchase_suggestion_items ADD COLUMN supplier_option_id BIGINT REFERENCES product_supplier_options(product_supplier_option_id);
ALTER TABLE purchase_suggestion_items ADD COLUMN purchase_item_id BIGINT REFERENCES purchase_items(purchase_item_id) ON DELETE SET NULL;
ALTER TABLE purchase_suggestion_items ADD COLUMN purchase_id BIGINT REFERENCES purchases(purchase_id) ON DELETE SET NULL;
ALTER TABLE purchase_suggestion_items ADD COLUMN reserved_stock NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN available_stock NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN target_stock NUMERIC(24,10);
ALTER TABLE purchase_suggestion_items ADD COLUMN covered_quantity NUMERIC(20,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN lead_days INTEGER;
ALTER TABLE purchase_suggestion_items ADD COLUMN selection_rule VARCHAR(30);
ALTER TABLE purchase_suggestion_items ADD COLUMN unit_cost NUMERIC(12,2);
ALTER TABLE purchase_suggestion_items ADD COLUMN base_factor NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN commercial_quantity NUMERIC(14,1);
ALTER TABLE purchase_suggestion_items ADD COLUMN result VARCHAR(30);
CREATE UNIQUE INDEX uq_suggestion_sku_warehouse ON purchase_suggestion_items(purchase_suggestion_id,product_id,warehouse_id);
CREATE INDEX ix_generated_purchase ON purchases(purchase_suggestion_id) WHERE purchase_suggestion_id IS NOT NULL;

INSERT INTO permissions(module,action,permission_key) VALUES ('purchases','suggest','purchases.suggest') ON CONFLICT(permission_key) DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.role_id,p.permission_id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key='purchases.suggest' AND EXISTS
 (SELECT 1 FROM role_permissions rp JOIN permissions e USING(permission_id) WHERE rp.role_id=r.role_id AND e.permission_key='purchases.create')
AND (SELECT count(DISTINCT e.permission_key) FROM role_permissions rp JOIN permissions e USING(permission_id)
 WHERE rp.role_id=r.role_id AND e.permission_key IN ('access.users.manage','access.roles.manage','security.settings.manage'))=3
ON CONFLICT DO NOTHING;

ALTER TABLE purchase_suggestion_items ADD COLUMN shortage_quantity NUMERIC(24,10);
