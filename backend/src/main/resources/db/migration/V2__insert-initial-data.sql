-- -- Inserts finais para banco NOVO: configuracoes, permissoes, cargos e dados ficticios.
-- -- 20 cenarios regionais, 20 catalogos (19 SIMPLE e 1 VARIANT), 21 SKUs.
-- -- Cada um dos 20 pedidos contem tres produtos distintos, com totais e historico correspondentes.
-- -- Estoque, compras e vendas com apresentacoes, snapshots, recibos e reservas consumidas.
-- -- Saldos de abertura e movimentos posteriores formam historico completo, reserva ativa zero.
-- -- Usuarios de exemplo continuam INATIVOS, com marcador sem credencial de login.
-- -- Nenhum administrador/senha, sessao, token, convite ou auditoria de acesso ficticios.
-- -- Financeiro/fiscal sao exemplos de desenvolvimento, sem emissao ou pagamento real.
--
-- INSERT INTO security_settings (id, idle_minutes, absolute_minutes, max_sessions)
-- VALUES (1, 60, 720, 2);
--
-- INSERT INTO permissions(module, action, permission_key) VALUES
-- ('customers', 'read', 'customers.read'),
-- ('customers', 'create', 'customers.create'),
-- ('customers', 'update', 'customers.update'),
-- ('customers', 'delete', 'customers.delete'),
-- ('suppliers', 'read', 'suppliers.read'),
-- ('suppliers', 'create', 'suppliers.create'),
-- ('suppliers', 'update', 'suppliers.update'),
-- ('suppliers', 'delete', 'suppliers.delete'),
-- ('products', 'read', 'products.read'),
-- ('products', 'create', 'products.create'),
-- ('products', 'update', 'products.update'),
-- ('products', 'delete', 'products.delete'),
-- ('warehouses', 'read', 'warehouses.read'),
-- ('warehouses', 'create', 'warehouses.create'),
-- ('warehouses', 'update', 'warehouses.update'),
-- ('warehouses', 'delete', 'warehouses.delete'),
-- ('stock', 'read', 'stock.read'),
-- ('stock', 'configure', 'stock.configure'),
-- ('stock', 'adjust', 'stock.adjust'),
-- ('prices', 'read', 'prices.read'),
-- ('prices', 'create', 'prices.create'),
-- ('prices', 'update', 'prices.update'),
-- ('prices', 'delete', 'prices.delete'),
-- ('orders', 'read', 'orders.read'),
-- ('orders', 'create', 'orders.create'),
-- ('orders', 'update', 'orders.update'),
-- ('orders', 'delete', 'orders.delete'),
-- ('orders', 'confirm', 'orders.confirm'),
-- ('orders', 'cancel', 'orders.cancel'),
-- ('orders', 'dispatch', 'orders.dispatch'),
-- ('orders', 'manualPrice', 'orders.manualPrice'),
-- ('orders', 'discount', 'orders.discount'),
-- ('purchases', 'read', 'purchases.read'),
-- ('purchases', 'create', 'purchases.create'),
-- ('purchases', 'update', 'purchases.update'),
-- ('purchases', 'delete', 'purchases.delete'),
-- ('purchases', 'send', 'purchases.send'),
-- ('purchases', 'confirm', 'purchases.confirm'),
-- ('purchases', 'cancel', 'purchases.cancel'),
-- ('purchases', 'receive', 'purchases.receive'),
-- ('access.users', 'manage', 'access.users.manage'),
-- ('access.roles', 'manage', 'access.roles.manage'),
-- ('security.settings', 'manage', 'security.settings.manage'),
-- ('security.sessions', 'manage', 'security.sessions.manage'),
-- ('security.audit', 'read', 'security.audit.read')
-- ON CONFLICT(permission_key) DO NOTHING;
--
-- -- DADOS FICTICIOS DE DESENVOLVIMENTO
--
-- DO $$
-- DECLARE
--     item RECORD;
--     role_key BIGINT;
--     user_key BIGINT;
--     category_key BIGINT;
--     brand_key BIGINT;
--     product_key BIGINT;
--     second_product_key BIGINT;
--     catalog_key BIGINT;
--     presentation_key BIGINT;
--     product_supplier_key BIGINT;
--     supplier_option_key BIGINT;
--     purchase_item_key BIGINT;
--     receipt_key BIGINT;
--     receipt_item_key BIGINT;
--     order_item_key BIGINT;
--     reservation_key BIGINT;
--     attributes_value TEXT;
--     seed_product_ids BIGINT[] := ARRAY[]::BIGINT[];
--     seed_order_ids BIGINT[] := ARRAY[]::BIGINT[];
--     order_index INTEGER;
--     product_offset INTEGER;
--     seeded_order RECORD;
--     extra_product RECORD;
--     extra_stock_key BIGINT;
--     extra_quantity NUMERIC(14,1);
--     opening_quantity NUMERIC(14,1);
--     order_total NUMERIC(12,2);
--     sku_number BIGINT;
--     product_sku TEXT;
--     warehouse_key BIGINT;
--     stock_key BIGINT;
--     supplier_key BIGINT;
--     price_table_key BIGINT;
--     customer_key BIGINT;
--     suggestion_key BIGINT;
--     purchase_key BIGINT;
--     order_key BIGINT;
--     revenue_key BIGINT;
--     expense_key BIGINT;
--     bank_key BIGINT;
--     receivable_key BIGINT;
--     payable_key BIGINT;
--     company_key BIGINT;
--     invoice_key BIGINT;
--     purchase_quantity NUMERIC(14,1);
--     sale_quantity NUMERIC(14,1);
--     final_balance NUMERIC(14,1);
--     purchase_total NUMERIC(12,2);
--     sale_total NUMERIC(12,2);
--     payment_fraction NUMERIC(3,2);
--     event_date DATE;
--     person_type_value VARCHAR(2);
--     customer_document VARCHAR(14);
-- BEGIN
--     FOR item IN
--         SELECT * FROM (VALUES
--             (1, 'Café torrado tradicional 500 g', 'Cafés e chás', 'Serra Clara', 'Torrefação Horizonte', 'Marina Albuquerque', 'Campinas', 'SP', '19', 12.40, 19.90),
--             (2, 'Arroz parboilizado 5 kg', 'Arroz e cereais', 'Campo Dourado', 'Cereais Rio Verde', 'Rafael Monteiro', 'Ribeirão Preto', 'SP', '16', 18.50, 28.90),
--             (3, 'Feijão carioca 1 kg', 'Feijões e leguminosas', 'Terra Mansa', 'Grãos Santa Helena', 'Beatriz Azevedo', 'Uberlândia', 'MG', '34', 5.80, 8.90),
--             (4, 'Macarrão espaguete 500 g', 'Massas secas', 'Vila Trigo', 'Massas Bela Mesa', 'Gustavo Farias', 'Juiz de Fora', 'MG', '32', 3.20, 5.49),
--             (5, 'Óleo de girassol 900 ml', 'Óleos e azeites', 'Sol do Vale', 'Óleos Primavera', 'Camila Nogueira', 'Londrina', 'PR', '43', 6.70, 10.90),
--             (6, 'Açúcar demerara 1 kg', 'Açúcares e adoçantes', 'Doce Estação', 'Engenho Aurora', 'Lucas Peixoto', 'Maringá', 'PR', '44', 4.10, 6.99),
--             (7, 'Farinha de mandioca 1 kg', 'Farinhas e féculas', 'Raiz Serena', 'Alimentos Raiz do Campo', 'Juliana Vasconcelos', 'Joinville', 'SC', '47', 4.60, 7.90),
--             (8, 'Biscoito integral 400 g', 'Biscoitos e snacks', 'Forno Alegre', 'Biscoitos Boa Fornada', 'Henrique Barros', 'Blumenau', 'SC', '47', 5.20, 8.49),
--             (9, 'Molho de tomate 300 g', 'Molhos e temperos', 'Horta Viva', 'Conservas Jardim Sul', 'Fernanda Siqueira', 'Caxias do Sul', 'RS', '54', 1.90, 3.29),
--             (10, 'Leite em pó integral 400 g', 'Leites e derivados', 'Pastos Altos', 'Laticínios Pedra Branca', 'Tiago Medeiros', 'Pelotas', 'RS', '53', 10.30, 16.90),
--             (11, 'Suco de uva integral 1 l', 'Sucos e néctares', 'Fruta Nobre', 'Bebidas Vale da Uva', 'Empório Ponto Certo', 'Sorocaba', 'SP', '15', 8.90, 14.90),
--             (12, 'Água mineral sem gás 1,5 l', 'Águas minerais', 'Fonte Cristal', 'Águas Serra Azul', 'Mercadinho Vila Nova', 'Bauru', 'SP', '14', 1.60, 2.99),
--             (13, 'Sabão em pó 800 g', 'Lavanderia', 'Brilho Leve', 'Química Limpa Casa', 'Comercial Flor do Bairro', 'Anápolis', 'GO', '62', 6.90, 11.90),
--             (14, 'Detergente neutro 500 ml', 'Limpeza de cozinha', 'Prato Limpo', 'Higiene Água Clara', 'Armazém Bom Vizinho', 'Dourados', 'MS', '67', 1.25, 2.39),
--             (15, 'Desinfetante lavanda 2 l', 'Limpeza de ambientes', 'Casa Fresca', 'Saneantes Porto Limpo', 'Mercearia Três Irmãos', 'Rondonópolis', 'MT', '66', 4.80, 8.49),
--             (16, 'Papel higiênico 12 rolos', 'Papéis sanitários', 'Toque Suave', 'Papéis Lago Branco', 'Loja Utilidades Sol Nascente', 'Feira de Santana', 'BA', '75', 8.30, 13.90),
--             (17, 'Sabonete vegetal 90 g', 'Higiene pessoal', 'Pele Serena', 'Cosméticos Flor Nativa', 'Conveniência Esquina Azul', 'Campina Grande', 'PB', '83', 1.80, 3.49),
--             (18, 'Creme dental menta 90 g', 'Cuidados bucais', 'Sorriso Vivo', 'Higiene Oral Novo Dia', 'Supermercado Canto Verde', 'Mossoró', 'RN', '84', 2.90, 5.29),
--             (19, 'Ração para cães adultos 3 kg', 'Alimentação animal', 'Amigo Fiel', 'Nutrição Animal Campo Bom', 'Pet Shop Patas e Companhia', 'Sobral', 'CE', '88', 19.70, 32.90),
--             (20, 'Saco de lixo reforçado 50 l', 'Descartáveis domésticos', 'Eco Prático', 'Embalagens Novo Ciclo', 'Distribuidora Bairro Unido', 'Caruaru', 'PE', '81', 3.60, 6.49)
--         ) AS data(n, product_name, category_name, brand_name, supplier_name,
--             customer_name, city, state, area_code, cost, price)
--     LOOP
--         sku_number := nextval('product_sku_seq');
--         product_sku := 'PRD-' || LPAD(sku_number::TEXT, GREATEST(6, LENGTH(sku_number::TEXT)), '0');
--
--         event_date := DATE '2026-09-01' + item.n;
--         purchase_quantity := 20 + item.n;
--         sale_quantity := 1 + item.n % 5;
--         final_balance := 80 + item.n * 3;
--         purchase_total := item.cost * purchase_quantity;
--         sale_total := item.price * sale_quantity;
--         payment_fraction := CASE WHEN item.n % 3 = 0 THEN 0.50 ELSE 1.00 END;
--         person_type_value := CASE WHEN item.n <= 10 THEN 'PF' ELSE 'PJ' END;
--         customer_document := CASE WHEN item.n <= 10
--             THEN '710000000' || LPAD(item.n::TEXT, 2, '0')
--             ELSE '7200000000' || LPAD(item.n::TEXT, 4, '0') END;
--
--         -- Acesso: perfil e operador próprios de cada centro regional.
--         INSERT INTO roles (name, description, created_at, active)
--         VALUES ('Operação ' || item.city, 'Perfil regional fictício de distribuição', TIMESTAMP '2026-08-01 08:00:00', TRUE)
--         RETURNING role_id INTO role_key;
--
--         -- Apenas permissoes reais de consulta para os perfis ficticios.
--         INSERT INTO role_permissions (role_id, permission_id)
--         SELECT role_key, permission_id FROM permissions
--         WHERE permission_key IN ('customers.read', 'suppliers.read', 'products.read',
--             'warehouses.read', 'stock.read', 'prices.read', 'orders.read', 'purchases.read');
--
--         INSERT INTO users (name, email, password_hash, created_at, role_id, active)
--         VALUES ('Operador Regional ' || item.city, 'operacao.' || item.n || '@example.com',
--             'SEED_ONLY_NO_LOGIN', TIMESTAMP '2026-08-01 08:00:00', role_key, FALSE)
--         RETURNING user_id INTO user_key;
--
--         INSERT INTO categories (name) VALUES (item.category_name) RETURNING category_id INTO category_key;
--         INSERT INTO brands (name) VALUES (item.brand_name) RETURNING brand_id INTO brand_key;
--
--         -- UN representa cada pacote comercial destes exemplos, explicitamente definido.
--         -- O ultimo catalogo demonstra duas variacoes de cor com SKUs proprios.
--         attributes_value := CASE WHEN item.n = 20 THEN '{"COLOR":"Preto"}' ELSE '{}' END;
--         INSERT INTO product_catalogs (name, brand_id, category_id, unit, type, active, created_at)
--         VALUES (item.product_name, brand_key, category_key, 'UN',
--             CASE WHEN item.n = 20 THEN 'VARIANT' ELSE 'SIMPLE' END,
--             item.n % 7 <> 0, TIMESTAMP '2026-08-01 08:00:00')
--         RETURNING product_catalog_id INTO catalog_key;
--         IF item.n = 20 THEN
--             INSERT INTO product_catalog_characteristics (product_catalog_id, characteristic)
--             VALUES (catalog_key, 'COLOR');
--         END IF;
--         INSERT INTO products (
--             catalog_id, variant_key, sku, cost, price, weight_kg, commercial_unit, origin, active, created_at
--         ) VALUES (catalog_key, CASE WHEN item.n = 20 THEN '{"COLOR":"preto"}' ELSE '' END,
--             product_sku, item.cost, item.price, 0.100 + item.n * 0.050,
--             'UN', 'NATIONAL', TRUE, TIMESTAMP '2026-08-01 08:00:00')
--         RETURNING product_id INTO product_key;
--         seed_product_ids := array_append(seed_product_ids, product_key);
--         IF item.n = 20 THEN
--             INSERT INTO product_variant_values (product_id, characteristic, value)
--             VALUES (product_key, 'COLOR', 'Preto');
--         END IF;
--         INSERT INTO product_presentations (product_id, name, base_factor, base_presentation, active)
--         VALUES (product_key, 'UN', 1, TRUE, TRUE)
--         RETURNING product_presentation_id INTO presentation_key;
--         INSERT INTO product_presentations (product_id, name, base_factor, base_presentation, active)
--         VALUES (product_key, 'Caixa 6', 6, FALSE, TRUE);
--
--         INSERT INTO warehouses (name, description)
--         VALUES ('Centro Logístico ' || item.city, 'Unidade regional de recebimento e expedição em ' || item.state)
--         RETURNING warehouse_id INTO warehouse_key;
--
--         INSERT INTO warehouse_addresses (
--             warehouse_id, state, city, street, neighborhood, number, complement, zip_code
--         ) VALUES (warehouse_key, item.state, item.city, 'Avenida das Indústrias', 'Distrito Industrial',
--             (100 + item.n)::TEXT, 'Galpão de distribuição', '00000-000');
--
--         INSERT INTO stock_items (
--             warehouse_id, product_id, quantity, reserved_quantity, min_quantity, max_quantity
--         ) VALUES (warehouse_key, product_key, final_balance, 0, 10 + item.n, 250 + item.n)
--         RETURNING stock_item_id INTO stock_key;
--
--         INSERT INTO suppliers (
--             legal_name, trade_name, tax_id, email, phone, whatsapp, avg_delivery_days, active, created_at
--         ) VALUES (item.supplier_name || ' Ltda.', item.supplier_name,
--             '7300000000' || LPAD(item.n::TEXT, 4, '0'),
--             CASE WHEN item.n % 4 = 0 THEN NULL ELSE 'comercial.' || item.n || '@example.com' END,
--             item.area_code || '3000' || LPAD(item.n::TEXT, 4, '0'),
--             CASE WHEN item.n % 3 = 0 THEN NULL ELSE item.area_code || '99000' || LPAD(item.n::TEXT, 4, '0') END,
--             CASE WHEN item.n % 6 = 0 THEN NULL ELSE 1 + item.n % 18 END,
--             item.n % 6 <> 0, TIMESTAMP '2026-08-01 08:00:00')
--         RETURNING supplier_id INTO supplier_key;
--
--         INSERT INTO supplier_addresses (
--             supplier_id, country, state, city, street, neighborhood, number, complement, zip_code, address_type
--         ) VALUES (supplier_key, 'Brasil', item.state, item.city, 'Rua dos Fabricantes', 'Distrito Industrial',
--             (200 + item.n)::TEXT, CASE WHEN item.n % 2 = 0 THEN NULL ELSE 'Bloco comercial' END,
--             '00000-000', 'MAIN');
--
--         INSERT INTO product_suppliers (product_id, supplier_id, preferred, active)
--         VALUES (product_key, supplier_key, item.n % 3 <> 0, item.n % 6 <> 0 AND item.n % 7 <> 0)
--         RETURNING product_supplier_id INTO product_supplier_key;
--         INSERT INTO product_supplier_options (
--             product_supplier_id, name, base_factor, base_presentation, cost,
--             avg_delivery_days, supplier_product_code, active, conversion_confirmed
--         ) VALUES (product_supplier_key, 'UN', 1, TRUE, item.cost, 1 + item.n % 18,
--             'FAB-' || product_sku, TRUE, TRUE)
--         RETURNING product_supplier_option_id INTO supplier_option_key;
--         INSERT INTO product_supplier_options (
--             product_supplier_id, name, base_factor, base_presentation, cost,
--             avg_delivery_days, supplier_product_code, active, conversion_confirmed
--         ) VALUES (product_supplier_key, 'Caixa 6', 6, FALSE, item.cost * 6,
--             1 + item.n % 18, 'CX6-' || product_sku, TRUE, TRUE);
--         INSERT INTO supplier_categories (supplier_id, category_id) VALUES (supplier_key, category_key);
--         INSERT INTO supplier_brands (supplier_id, brand_id) VALUES (supplier_key, brand_key);
--
--         INSERT INTO price_tables (name, start_date, end_date, active, created_at)
--         VALUES ('Condição Comercial ' || item.city, DATE '2026-08-01', DATE '2027-07-31',
--             item.n % 8 <> 0, TIMESTAMP '2026-08-01 08:00:00')
--         RETURNING price_table_id INTO price_table_key;
--         INSERT INTO price_table_items (price_table_id, product_id, price)
--         VALUES (price_table_key, product_key, item.price);
--         INSERT INTO price_table_items (price_table_id, product_id, presentation_id, price)
--         SELECT price_table_key, product_key, product_presentation_id, item.price * 6
--         FROM product_presentations WHERE product_id = product_key AND NOT base_presentation;
--
--         INSERT INTO customers (
--             person_type, tax_id, legal_name, trade_name, birth_date, email,
--             phone, whatsapp, active, price_table_id, created_at
--         ) VALUES (person_type_value, customer_document,
--             item.customer_name || CASE WHEN person_type_value = 'PJ' THEN ' Ltda.' ELSE '' END,
--             CASE WHEN person_type_value = 'PJ' THEN item.customer_name ELSE NULL END,
--             CASE WHEN person_type_value = 'PF' THEN DATE '1980-04-15' + item.n * 470 ELSE NULL END,
--             CASE WHEN item.n % 4 = 0 THEN NULL ELSE 'cliente.regional.' || item.n || '@example.com' END,
--             item.area_code || '98000' || LPAD(item.n::TEXT, 4, '0'),
--             CASE WHEN item.n % 3 = 0 THEN NULL ELSE item.area_code || '98000' || LPAD(item.n::TEXT, 4, '0') END,
--             item.n % 9 <> 0, price_table_key, TIMESTAMP '2026-08-01 08:00:00')
--         RETURNING customer_id INTO customer_key;
--
--         INSERT INTO customer_addresses (
--             customer_id, state, city, street, neighborhood, number, complement, zip_code, address_type
--         ) VALUES (customer_key, item.state, item.city, 'Rua do Comércio', 'Centro',
--             (300 + item.n)::TEXT, CASE WHEN person_type_value = 'PJ' THEN 'Loja térrea' ELSE 'Casa' END,
--             '00000-000', 'MAIN');
--
--         INSERT INTO financial_categories (name, type, active)
--         VALUES ('Receita regional - ' || item.category_name, 'REVENUE', TRUE)
--         RETURNING financial_category_id INTO revenue_key;
--         INSERT INTO financial_categories (name, type, active)
--         VALUES ('Aquisição regional - ' || item.category_name, 'EXPENSE', TRUE)
--         RETURNING financial_category_id INTO expense_key;
--
--         INSERT INTO bank_accounts (name, type, initial_balance, active)
--         VALUES ('Conta Regional ' || item.city,
--             CASE WHEN item.n % 3 = 0 THEN 'CASH' WHEN item.n % 3 = 1 THEN 'CHECKING_ACCOUNT' ELSE 'SAVINGS_ACCOUNT' END,
--             10000.00 + item.n * 500.00, TRUE)
--         RETURNING bank_account_id INTO bank_key;
--
--         INSERT INTO companies (
--             legal_name, trade_name, tax_id, tax_regime, nfe_environment,
--             nfe_default_series, nfe_next_number
--         ) VALUES ('Distribuidora ' || item.brand_name || ' Regional Ltda.',
--             'Distribuidora ' || item.brand_name, '7400000000' || LPAD(item.n::TEXT, 4, '0'),
--             'SIMPLES_NACIONAL', 'HOMOLOGATION', '1', 2)
--         RETURNING company_id INTO company_key;
--
--         INSERT INTO company_addresses (
--             company_id, country, state, city, street, neighborhood, number, complement, zip_code
--         ) VALUES (company_key, 'Brasil', item.state, item.city, 'Avenida Regional', 'Centro',
--             (400 + item.n)::TEXT, 'Sede administrativa', '00000-000');
--
--         INSERT INTO purchase_suggestions (generated_at, updated_at, reference_period, status, reviewer_user_id)
--         VALUES (event_date - 3, event_date - 2, '2026-08', 'APPROVED', user_key)
--         RETURNING purchase_suggestion_id INTO suggestion_key;
--
--         INSERT INTO purchase_suggestion_items (
--             purchase_suggestion_id, product_id, supplier_id, period_sold_quantity,
--             avg_daily_consumption, current_stock, min_stock, suggested_quantity
--         ) VALUES (suggestion_key, product_key, supplier_key, 30 + item.n,
--             ROUND((30.00 + item.n) / 30.00, 2), final_balance + sale_quantity - purchase_quantity,
--             10 + item.n, purchase_quantity);
--
--         INSERT INTO purchases (
--             supplier_id, user_id, purchase_suggestion_id, status, created_at,
--             updated_at, expected_delivery_date, total_amount
--         ) VALUES (supplier_key, user_key, suggestion_key, 'RECEIVED', event_date - 2,
--             event_date, event_date, purchase_total)
--         RETURNING purchase_id INTO purchase_key;
--
--         INSERT INTO purchase_items (
--             purchase_id, product_id, warehouse_id, quantity, unit_cost, received_quantity,
--             supplier_option_id, base_quantity, presentation_name_snapshot, base_factor_snapshot,
--             unit_snapshot, product_name_snapshot, sku_snapshot, attributes_snapshot
--         ) VALUES (purchase_key, product_key, warehouse_key, purchase_quantity, item.cost, purchase_quantity,
--             supplier_option_key, purchase_quantity, 'UN', 1, 'UN', item.product_name, product_sku, attributes_value)
--         RETURNING purchase_item_id INTO purchase_item_key;
--         INSERT INTO purchase_receipts (purchase_id, request_id, operator_user_id, received_at, notes)
--         VALUES (purchase_key, md5('jamshub-initial-purchase-' || item.n::TEXT)::UUID,
--             user_key, event_date, 'Recebimento ficticio regional completo')
--         RETURNING purchase_receipt_id INTO receipt_key;
--         INSERT INTO purchase_receipt_items (purchase_receipt_id, purchase_item_id, stock_item_id, quantity)
--         VALUES (receipt_key, purchase_item_key, stock_key, purchase_quantity)
--         RETURNING purchase_receipt_item_id INTO receipt_item_key;
--
--         INSERT INTO orders (total_amount, payment_method, status, created_at, customer_id, user_id)
--         VALUES (sale_total, CASE WHEN item.n % 2 = 0 THEN 'BOLETO' ELSE 'PIX' END,
--             'INVOICED', event_date + 1, customer_key, user_key)
--         RETURNING order_id INTO order_key;
--         seed_order_ids := array_append(seed_order_ids, order_key);
--         INSERT INTO order_items (
--             order_id, product_id, warehouse_id, quantity, unit_price, discount, presentation_id,
--             base_quantity, presentation_name_snapshot, base_factor_snapshot, unit_snapshot,
--             product_name_snapshot, sku_snapshot, attributes_snapshot
--         ) VALUES (order_key, product_key, warehouse_key, sale_quantity, item.price, 0.00,
--             presentation_key, sale_quantity, 'UN', 1, 'UN', item.product_name, product_sku, attributes_value)
--         RETURNING order_item_id INTO order_item_key;
--         INSERT INTO stock_reservations (order_item_id, stock_item_id, quantity, status, created_at, updated_at)
--         VALUES (order_item_key, stock_key, sale_quantity, 'CONSUMED', event_date + 1, event_date + 1)
--         RETURNING stock_reservation_id INTO reservation_key;
--
--         -- Saldo de abertura tambem documentado, antes da compra e da saida.
--         INSERT INTO stock_movements (
--             stock_item_id, type, reason, quantity, previous_balance, new_balance, user_id, occurred_at, notes
--         ) VALUES (stock_key, 'INBOUND', 'MANUAL_ADJUSTMENT', final_balance + sale_quantity - purchase_quantity,
--             0, final_balance + sale_quantity - purchase_quantity, user_key, event_date - 1,
--             'Saldo de abertura ficticio regional');
--
--         -- Entrada e saída terminam exatamente no saldo cadastrado em stock_items.
--         INSERT INTO stock_movements (
--             stock_item_id, type, reason, quantity, previous_balance, new_balance,
--             purchase_id, user_id, occurred_at, purchase_receipt_item_id, notes
--         ) VALUES (stock_key, 'INBOUND', 'PURCHASE', purchase_quantity,
--             final_balance + sale_quantity - purchase_quantity, final_balance + sale_quantity,
--             purchase_key, user_key, event_date, receipt_item_key, 'Recebimento regional de ' || item.product_name);
--         INSERT INTO stock_movements (
--             stock_item_id, type, reason, quantity, previous_balance, new_balance,
--             order_id, user_id, occurred_at, stock_reservation_id, notes
--         ) VALUES (stock_key, 'OUTBOUND', 'SALE', sale_quantity,
--             final_balance + sale_quantity, final_balance, order_key, user_key,
--             event_date + 1, reservation_key, 'Expedição para ' || item.customer_name);
--
--         INSERT INTO accounts_receivable (
--             customer_id, order_id, financial_category_id, description, amount, due_date, status, issued_at
--         ) VALUES (customer_key, order_key, revenue_key, 'Venda regional - ' || item.product_name,
--             sale_total, event_date + 8, CASE WHEN payment_fraction = 1.00 THEN 'PAID' ELSE 'PARTIAL' END, event_date + 1)
--         RETURNING account_receivable_id INTO receivable_key;
--         INSERT INTO accounts_receivable_payments (
--             account_receivable_id, bank_account_id, user_id, paid_amount, paid_at, payment_method, notes
--         ) VALUES (receivable_key, bank_key, user_key, ROUND(sale_total * payment_fraction, 2),
--             event_date + 2, CASE WHEN item.n % 2 = 0 THEN 'BOLETO' ELSE 'PIX' END, 'Recebimento fictício regional');
--
--         INSERT INTO accounts_payable (
--             supplier_id, purchase_id, financial_category_id, description, amount, due_date, status, issued_at
--         ) VALUES (supplier_key, purchase_key, expense_key, 'Aquisição regional - ' || item.product_name,
--             purchase_total, event_date + 10, CASE WHEN payment_fraction = 1.00 THEN 'PAID' ELSE 'PARTIAL' END, event_date)
--         RETURNING account_payable_id INTO payable_key;
--         INSERT INTO accounts_payable_payments (
--             account_payable_id, bank_account_id, user_id, paid_amount, paid_at, payment_method, notes
--         ) VALUES (payable_key, bank_key, user_key, ROUND(purchase_total * payment_fraction, 2),
--             event_date + 2, 'BANK_TRANSFER', 'Pagamento fictício ao fabricante regional');
--
--         -- Documentos de homologação ilustrativos, sem XML ou chave de acesso reais.
--         INSERT INTO invoices (
--             order_id, company_id, customer_id, number, series, operation_nature,
--             purpose, environment, status, authorization_protocol, issued_at,
--             authorized_at, products_total_amount, total_amount
--         ) VALUES (order_key, company_key, customer_key, 1, '1', 'Venda de mercadorias regionais',
--             'NORMAL', 'HOMOLOGATION', 'AUTHORIZED', 'SIMULADO-REG-' || LPAD(item.n::TEXT, 2, '0'),
--             event_date + 1, event_date + 1, sale_total, sale_total)
--         RETURNING invoice_id INTO invoice_key;
--         INSERT INTO invoice_items (
--             invoice_id, product_id, quantity, unit_price, total_price,
--             cfop_snapshot, cst_csosn_snapshot, icms_rate_snapshot, icms_amount,
--             ipi_rate_snapshot, ipi_amount, pis_rate_snapshot, pis_amount,
--             cofins_rate_snapshot, cofins_amount
--         ) VALUES (invoice_key, product_key, sale_quantity, item.price, sale_total,
--             '5102', '102', 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00);
--         INSERT INTO invoice_events (invoice_id, event_type, occurred_at, protocol, description)
--         VALUES (invoice_key, 'CORRECTION_LETTER', event_date + 2,
--             'SIMULADO-CC-' || LPAD(item.n::TEXT, 2, '0'), 'Correção ilustrativa da descrição de entrega regional');
--
--         IF item.n = 20 THEN
--             sku_number := nextval('product_sku_seq');
--             product_sku := 'PRD-' || LPAD(sku_number::TEXT, GREATEST(6, LENGTH(sku_number::TEXT)), '0');
--             INSERT INTO products (catalog_id, variant_key, sku, cost, price, active, created_at)
--             VALUES (catalog_key, '{"COLOR":"azul"}', product_sku, item.cost + 0.50, item.price + 1.00,
--                 TRUE, TIMESTAMP '2026-08-01 08:00:00')
--             RETURNING product_id INTO second_product_key;
--             INSERT INTO product_variant_values (product_id, characteristic, value)
--             VALUES (second_product_key, 'COLOR', 'Azul');
--             INSERT INTO product_presentations (product_id, name, base_factor, base_presentation)
--             VALUES (second_product_key, 'UN', 1, TRUE), (second_product_key, 'Caixa 6', 6, FALSE);
--             INSERT INTO stock_items (warehouse_id, product_id, quantity, reserved_quantity, min_quantity)
--             VALUES (warehouse_key, second_product_key, 0, 0, 10);
--             INSERT INTO product_suppliers (product_id, supplier_id, preferred, active)
--             VALUES (second_product_key, supplier_key, TRUE, TRUE)
--             RETURNING product_supplier_id INTO product_supplier_key;
--             INSERT INTO product_supplier_options (
--                 product_supplier_id, name, base_factor, base_presentation, cost, active, conversion_confirmed
--             ) VALUES (product_supplier_key, 'UN', 1, TRUE, item.cost + 0.50, TRUE, TRUE),
--                 (product_supplier_key, 'Caixa 6', 6, FALSE, (item.cost + 0.50) * 6, TRUE, TRUE);
--         END IF;
--     END LOOP;
--
--     -- Os catalogos precisam existir antes de montar os pedidos com varios produtos.
--     -- Os IDs sao os retornados acima; nao se presume igualdade entre sequencias.
--     FOR order_index IN 1..cardinality(seed_order_ids) LOOP
--         SELECT o.order_id, o.user_id, o.created_at, oi.warehouse_id, i.invoice_id
--         INTO seeded_order
--         FROM orders o
--         JOIN order_items oi ON oi.order_id = o.order_id
--         JOIN invoices i ON i.order_id = o.order_id
--         WHERE o.order_id = seed_order_ids[order_index];
--
--         FOR product_offset IN 1..2 LOOP
--             SELECT p.product_id, p.sku, p.price, c.name, c.unit,
--                 pp.product_presentation_id,
--                 COALESCE((
--                     SELECT jsonb_object_agg(v.characteristic, v.value)::TEXT
--                     FROM product_variant_values v WHERE v.product_id = p.product_id
--                 ), '{}') AS attributes_snapshot
--             INTO extra_product
--             FROM products p
--             JOIN product_catalogs c ON c.product_catalog_id = p.catalog_id
--             JOIN product_presentations pp ON pp.product_id = p.product_id AND pp.base_presentation
--             WHERE p.product_id = seed_product_ids[
--                 ((order_index + product_offset - 1) % cardinality(seed_product_ids)) + 1
--             ];
--
--             extra_quantity := product_offset + order_index % 3;
--             opening_quantity := 30 + (order_index % 5) * 5;
--
--             -- Estoque no mesmo deposito do pedido, com abertura e saida rastreaveis.
--             INSERT INTO stock_items (
--                 warehouse_id, product_id, quantity, reserved_quantity, min_quantity, max_quantity
--             ) VALUES (seeded_order.warehouse_id, extra_product.product_id,
--                 opening_quantity - extra_quantity, 0, 10, 250)
--             RETURNING stock_item_id INTO extra_stock_key;
--
--             INSERT INTO order_items (
--                 order_id, product_id, warehouse_id, quantity, unit_price, discount,
--                 presentation_id, base_quantity, presentation_name_snapshot, base_factor_snapshot,
--                 unit_snapshot, product_name_snapshot, sku_snapshot, attributes_snapshot
--             ) VALUES (seeded_order.order_id, extra_product.product_id, seeded_order.warehouse_id,
--                 extra_quantity, extra_product.price, 0, extra_product.product_presentation_id,
--                 extra_quantity, 'UN', 1, extra_product.unit, extra_product.name,
--                 extra_product.sku, extra_product.attributes_snapshot)
--             RETURNING order_item_id INTO order_item_key;
--
--             INSERT INTO stock_reservations (
--                 order_item_id, stock_item_id, quantity, status, created_at, updated_at
--             ) VALUES (order_item_key, extra_stock_key, extra_quantity, 'CONSUMED',
--                 seeded_order.created_at, seeded_order.created_at)
--             RETURNING stock_reservation_id INTO reservation_key;
--
--             INSERT INTO stock_movements (
--                 stock_item_id, type, reason, quantity, previous_balance, new_balance,
--                 user_id, occurred_at, notes
--             ) VALUES (extra_stock_key, 'INBOUND', 'MANUAL_ADJUSTMENT', opening_quantity,
--                 0, opening_quantity, seeded_order.user_id, seeded_order.created_at - INTERVAL '1 day',
--                 'Saldo de abertura ficticio para pedido com varios produtos');
--
--             INSERT INTO stock_movements (
--                 stock_item_id, type, reason, quantity, previous_balance, new_balance,
--                 order_id, user_id, occurred_at, stock_reservation_id, notes
--             ) VALUES (extra_stock_key, 'OUTBOUND', 'SALE', extra_quantity,
--                 opening_quantity, opening_quantity - extra_quantity, seeded_order.order_id,
--                 seeded_order.user_id, seeded_order.created_at, reservation_key,
--                 'Expedicao ficticia de item adicional do pedido');
--
--             INSERT INTO invoice_items (
--                 invoice_id, product_id, quantity, unit_price, total_price,
--                 cfop_snapshot, cst_csosn_snapshot, icms_rate_snapshot, icms_amount,
--                 ipi_rate_snapshot, ipi_amount, pis_rate_snapshot, pis_amount,
--                 cofins_rate_snapshot, cofins_amount
--             ) VALUES (seeded_order.invoice_id, extra_product.product_id, extra_quantity,
--                 extra_product.price, ROUND(extra_quantity * extra_product.price, 2),
--                 '5102', '102', 0, 0, 0, 0, 0, 0, 0, 0);
--         END LOOP;
--
--         SELECT SUM(ROUND(quantity * unit_price, 2)) INTO order_total
--         FROM order_items WHERE order_id = seeded_order.order_id;
--
--         UPDATE orders SET total_amount = order_total WHERE order_id = seeded_order.order_id;
--         UPDATE invoices SET products_total_amount = order_total, total_amount = order_total
--         WHERE invoice_id = seeded_order.invoice_id;
--         UPDATE accounts_receivable
--         SET amount = order_total, description = 'Venda regional com tres produtos distintos'
--         WHERE order_id = seeded_order.order_id;
--         UPDATE accounts_receivable_payments payment
--         SET paid_amount = ROUND(order_total * CASE WHEN order_index % 3 = 0 THEN 0.50 ELSE 1.00 END, 2)
--         FROM accounts_receivable receivable
--         WHERE receivable.order_id = seeded_order.order_id
--             AND payment.account_receivable_id = receivable.account_receivable_id;
--     END LOOP;
-- END;
-- $$;
--
-- -- Os cargos regionais continuam antes dos modelos, preservando a ordem anterior de IDs.
-- -- Modelos iniciais de acesso. Nenhum usuario recebe acesso administrativo por estes inserts.
-- DO $$
-- DECLARE administrator BIGINT; sales BIGINT; purchasing BIGINT; inventory BIGINT;
-- BEGIN
--     INSERT INTO roles(name, description) VALUES ('Administrador', 'Initial configurable access template') RETURNING role_id INTO administrator;
--     INSERT INTO roles(name, description) VALUES ('Vendas', 'Initial configurable access template') RETURNING role_id INTO sales;
--     INSERT INTO roles(name, description) VALUES ('Compras', 'Initial configurable access template') RETURNING role_id INTO purchasing;
--     INSERT INTO roles(name, description) VALUES ('Estoque', 'Initial configurable access template') RETURNING role_id INTO inventory;
--     INSERT INTO roles(name, description) VALUES ('Financeiro', 'Initial configurable access template');
--     INSERT INTO role_permissions SELECT administrator, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'products.create', 'products.update', 'products.delete', 'warehouses.read', 'warehouses.create', 'warehouses.update', 'warehouses.delete', 'stock.read', 'stock.configure', 'stock.adjust', 'prices.read', 'prices.create', 'prices.update', 'prices.delete', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.dispatch', 'orders.manualPrice', 'orders.discount', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel', 'purchases.receive', 'access.users.manage', 'access.roles.manage', 'security.settings.manage', 'security.sessions.manage', 'security.audit.read');
--     INSERT INTO role_permissions SELECT sales, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'products.read', 'warehouses.read', 'stock.read', 'prices.read', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.manualPrice', 'orders.discount');
--     INSERT INTO role_permissions SELECT purchasing, permission_id FROM permissions WHERE permission_key IN ('suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'warehouses.read', 'stock.read', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel');
--     INSERT INTO role_permissions SELECT inventory, permission_id FROM permissions WHERE permission_key IN ('products.read', 'warehouses.read', 'stock.read', 'stock.adjust', 'orders.read', 'orders.dispatch', 'purchases.read', 'purchases.receive');
-- END $$;
--


-- Flyway: V2__insert_initial_data.sql
-- Dados demonstrativos FICTICIOS de uma distribuidora de motopecas.
-- Executar SOMENTE em banco novo apos V1; nao aplicar em producao.
-- Documentos, contatos, enderecos e empresa sao ficticios.
-- Valores fiscais (NCM, tributos) nao constituem parametrizacao fiscal valida.
-- Nenhuma NFe e autorizada ou transmitida por esta migration.
-- Usuario historico INATIVO, marcador sem credencial; nenhum admin criado.
-- Permissoes, cargos-modelo e configuracoes de sessao conforme insert existente.
-- Sem criacao de sessao, token, convite, desafio, auditoria ou NFe autorizada.


-- Configuracao de seguranca EXATAMENTE conforme o insert de referencia.
INSERT INTO security_settings (id, idle_minutes, absolute_minutes, max_sessions)
VALUES (1, 60, 720, 2);

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

-- Cargos-modelo e concessoes: preservados do insert atual.
-- Financeiro e criado SEM permissoes, conforme o arquivo recebido.
DO $$
DECLARE administrator BIGINT; sales BIGINT; purchasing BIGINT; inventory BIGINT;
BEGIN
    INSERT INTO roles(name, description) VALUES ('Administrador', 'Initial configurable access template') RETURNING role_id INTO administrator;
    INSERT INTO roles(name, description) VALUES ('Vendas', 'Initial configurable access template') RETURNING role_id INTO sales;
    INSERT INTO roles(name, description) VALUES ('Compras', 'Initial configurable access template') RETURNING role_id INTO purchasing;
    INSERT INTO roles(name, description) VALUES ('Estoque', 'Initial configurable access template') RETURNING role_id INTO inventory;
    INSERT INTO roles(name, description) VALUES ('Financeiro', 'Initial configurable access template');
    INSERT INTO role_permissions SELECT administrator, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'products.create', 'products.update', 'products.delete', 'warehouses.read', 'warehouses.create', 'warehouses.update', 'warehouses.delete', 'stock.read', 'stock.configure', 'stock.adjust', 'prices.read', 'prices.create', 'prices.update', 'prices.delete', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.dispatch', 'orders.manualPrice', 'orders.discount', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel', 'purchases.receive', 'access.users.manage', 'access.roles.manage', 'security.settings.manage', 'security.sessions.manage', 'security.audit.read');
    INSERT INTO role_permissions SELECT sales, permission_id FROM permissions WHERE permission_key IN ('customers.read', 'customers.create', 'customers.update', 'customers.delete', 'products.read', 'warehouses.read', 'stock.read', 'prices.read', 'orders.read', 'orders.create', 'orders.update', 'orders.delete', 'orders.confirm', 'orders.cancel', 'orders.manualPrice', 'orders.discount');
    INSERT INTO role_permissions SELECT purchasing, permission_id FROM permissions WHERE permission_key IN ('suppliers.read', 'suppliers.create', 'suppliers.update', 'suppliers.delete', 'products.read', 'warehouses.read', 'stock.read', 'purchases.read', 'purchases.create', 'purchases.update', 'purchases.delete', 'purchases.send', 'purchases.confirm', 'purchases.cancel');
    INSERT INTO role_permissions SELECT inventory, permission_id FROM permissions WHERE permission_key IN ('products.read', 'warehouses.read', 'stock.read', 'stock.adjust', 'orders.read', 'orders.dispatch', 'purchases.read', 'purchases.receive');
END $$;

-- Usuario tecnico ficticio DESATIVADO, sem papel administrativo nem senha valida.
-- Usado exclusivamente como autor historico de operacoes de desenvolvimento.
INSERT INTO roles (name, description, active)
VALUES ('Operador Seed (sem acesso)', 'Perfil ficticio, apenas para referencias historicas', TRUE);
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Operador Seed (sem acesso)'
  AND p.permission_key IN ('customers.read', 'suppliers.read', 'products.read',
    'warehouses.read', 'stock.read', 'prices.read', 'orders.read', 'purchases.read');
INSERT INTO users (name, email, password_hash, role_id, active)
SELECT 'Operador demonstrativo (desativado)', 'operador-seed@example.invalid',
       'SEED_ONLY_NO_LOGIN', role_id, FALSE
FROM roles WHERE name = 'Operador Seed (sem acesso)';

INSERT INTO categories (name) VALUES ('Pneus e Camaras'),
('Transmissao'),
('Freios'),
('Motor'),
('Filtros'),
('Eletrica'),
('Suspensao'),
('Lubrificantes'),
('Cabos e Comandos'),
('Acessorios');


INSERT INTO brands (name) VALUES ('Levorin'),
('Riffel'),
('Cobreq'),
('NGK'),
('Magnetron'),
('Vini'),
('Metal Leve'),
('Tecfil'),
('Motul'),
('Cofap'),
('Vedamotors'),
('GVS'),
('Pro Tork'),
('Brosol');


CREATE TEMP TABLE seed_products (
    name TEXT, brand TEXT, category TEXT, cost NUMERIC(12,2),
    price NUMERIC(12,2), initial_qty NUMERIC(14,1), ncm VARCHAR(8)
) ON COMMIT DROP;
INSERT INTO seed_products VALUES
('Pneu dianteiro 90/90-18','Levorin','Pneus e Camaras',135,199,36,'40114000'),
('Pneu traseiro 110/90-17','Levorin','Pneus e Camaras',165,239,29,'40114000'),
('Camara de ar aro 18','Levorin','Pneus e Camaras',23,38,95,'40139000'),
('Camara de ar aro 17','Levorin','Pneus e Camaras',22,37,91,'40139000'),
('Kit relacao CG 160','Riffel','Transmissao',94,149,65,'87141000'),
('Kit relacao Fazer 250','Riffel','Transmissao',125,199,42,'87141000'),
('Corrente transmissao 428H','Riffel','Transmissao',54,89,56,'73151100'),
('Pinhao 15 dentes CG','Riffel','Transmissao',15,28,85,'87141000'),
('Pastilha freio dianteira CG','Cobreq','Freios',28,49,90,'87141000'),
('Pastilha freio dianteira Fazer','Cobreq','Freios',38,65,61,'87141000'),
('Lona freio traseira CG','Cobreq','Freios',18,34,105,'87141000'),
('Disco freio dianteiro CG','Cobreq','Freios',72,119,27,'87141000'),
('Vela ignicao CG 160','NGK','Eletrica',16,29,130,'85111000'),
('Vela ignicao Fazer 250','NGK','Eletrica',24,43,75,'85111000'),
('Bobina ignicao CG 160','Magnetron','Eletrica',58,95,39,'85113000'),
('Regulador retificador CG','Magnetron','Eletrica',63,109,32,'85118090'),
('Bateria 12V 5Ah','Vini','Eletrica',119,185,21,'85071090'),
('Lampada farol H4 12V','GVS','Eletrica',15,29,92,'85392110'),
('Pistao kit CG 160','Metal Leve','Motor',138,219,24,'84099190'),
('Jogo aneis CG 160','Metal Leve','Motor',49,85,39,'84099190'),
('Jogo juntas motor CG','Vedamotors','Motor',37,69,59,'84849000'),
('Filtro ar CG 160','Tecfil','Filtros',19,36,121,'84213100'),
('Filtro oleo Fazer 250','Tecfil','Filtros',15,29,110,'84212300'),
('Oleo motor 10W30 1L','Motul','Lubrificantes',28,45,160,'27101932'),
('Oleo motor 20W50 1L','Motul','Lubrificantes',24,39,182,'27101932'),
('Amortecedor traseiro CG par','Cofap','Suspensao',199,310,17,'87141000'),
('Retentor bengala CG par','Vedamotors','Suspensao',22,39,78,'40169300'),
('Cabo embreagem CG 160','Brosol','Cabos e Comandos',19,35,86,'87141000'),
('Cabo acelerador CG 160','Brosol','Cabos e Comandos',17,32,92,'87141000'),
('Manopla esportiva par','Pro Tork','Acessorios',15,32,66,'87141000'),
('Retrovisor universal par','Pro Tork','Acessorios',29,58,72,'70091000'),
('Capacete aberto tamanho M','Pro Tork','Acessorios',95,159,24,'65061000');


INSERT INTO product_catalogs (name, brand_id, category_id, unit, type)
SELECT s.name, b.brand_id, c.category_id, 'UN', 'SIMPLE'
FROM seed_products s JOIN brands b ON b.name=s.brand
JOIN categories c ON c.name=s.category;

INSERT INTO products (catalog_id, variant_key, sku, cost, price, weight_kg,
                      ncm, origin, commercial_unit)
SELECT pc.product_catalog_id, '', 'SKU-' || lpad(nextval('product_sku_seq')::TEXT, 6, '0'),
       s.cost, s.price, 0.250, s.ncm, 'NATIONAL', 'UN'
FROM seed_products s JOIN product_catalogs pc ON pc.name=s.name;

INSERT INTO product_presentations (product_id, name, base_factor, base_presentation)
SELECT p.product_id, 'Unidade', 1, TRUE FROM products p
JOIN product_catalogs pc ON pc.product_catalog_id=p.catalog_id
JOIN seed_products s ON s.name=pc.name;

-- Multipack opcional para itens de alto giro.
INSERT INTO product_presentations (product_id, name, base_factor, base_presentation, price)
SELECT p.product_id, 'Caixa com 10 unidades', 10, FALSE, round(p.price * 9.5, 2)
FROM products p JOIN product_catalogs pc ON pc.product_catalog_id=p.catalog_id
WHERE pc.name IN ('Vela ignicao CG 160','Filtro ar CG 160','Lampada farol H4 12V');


INSERT INTO warehouses (name, description) VALUES
('CD Principal', 'Centro de distribuicao e expedicao'),
('Loja Balcao', 'Estoque de pronta entrega');

INSERT INTO warehouse_addresses (warehouse_id, state, city, street, neighborhood, number, complement, zip_code)
SELECT warehouse_id, 'PE', 'Caruaru', 'Rua Exemplo Industrial', 'Distrito Industrial', '100', 'Galpao ficticio', '55000-000'
FROM warehouses WHERE name='CD Principal';
INSERT INTO warehouse_addresses (warehouse_id, state, city, street, neighborhood, number, complement, zip_code)
SELECT warehouse_id, 'PE', 'Caruaru', 'Av Exemplo Comercial', 'Centro', '200', 'Ponto ficticio', '55000-001'
FROM warehouses WHERE name='Loja Balcao';

INSERT INTO stock_items (warehouse_id, product_id, quantity, reserved_quantity, min_quantity, max_quantity)
SELECT w.warehouse_id, p.product_id,
       CASE WHEN w.name='CD Principal' THEN s.initial_qty ELSE greatest(3, floor(s.initial_qty / 5)) END,
       0,
       CASE WHEN w.name='CD Principal' THEN 15 ELSE 3 END,
       CASE WHEN w.name='CD Principal' THEN 250 ELSE 60 END
FROM products p JOIN product_catalogs pc ON pc.product_catalog_id=p.catalog_id
JOIN seed_products s ON s.name=pc.name CROSS JOIN warehouses w;

-- O saldo inicial e registrado com movimentos de ajuste auditaveis.
INSERT INTO stock_movements (stock_item_id, type, reason, quantity, previous_balance,
                             new_balance, user_id, occurred_at, notes)
SELECT si.stock_item_id, 'INBOUND', 'MANUAL_ADJUSTMENT', si.quantity, 0,
       si.quantity, u.user_id, CURRENT_TIMESTAMP - INTERVAL '35 days',
       'Carga inicial demonstrativa'
FROM stock_items si CROSS JOIN users u WHERE u.email='operador-seed@example.invalid';


INSERT INTO suppliers (legal_name,trade_name,tax_id,email,phone,avg_delivery_days) VALUES
('Moto Distribuidora Nordeste Ltda','Moto Nordeste','00000000000001','contato1@example.invalid','81990000001',7),
('Pecas Rota 101 Ltda','Rota 101','00000000000002','contato2@example.invalid','81990000002',9),
('Atacado Duas Rodas Ltda','Duas Rodas','00000000000003','contato3@example.invalid','81990000003',6),
('Central das Motopecas Ltda','Central Moto','00000000000004','contato4@example.invalid','81990000004',12),
('Logistica e Pecas Estrada Ltda','Estrada Pecas','00000000000005','contato5@example.invalid','81990000005',5),
('Distribuidora Giro Forte Ltda','Giro Forte','00000000000006','contato6@example.invalid','81990000006',8);


INSERT INTO supplier_addresses (supplier_id,country,state,city,street,neighborhood,number,complement,zip_code,address_type)
SELECT supplier_id,'Brasil','PE','Recife','Rua Fornecedor Exemplo','Centro',supplier_id::TEXT,NULL,'50000-000','MAIN' FROM suppliers;

INSERT INTO supplier_categories (supplier_id, category_id)
SELECT s.supplier_id,c.category_id FROM suppliers s CROSS JOIN categories c
WHERE (s.supplier_id + c.category_id) % 3 <> 0;
INSERT INTO supplier_brands (supplier_id, brand_id)
SELECT s.supplier_id,b.brand_id FROM suppliers s CROSS JOIN brands b
WHERE (s.supplier_id + b.brand_id) % 3 <> 0;

-- Cada produto possui dois fornecedores, sendo apenas um preferencial.
INSERT INTO product_suppliers (product_id,supplier_id,preferred)
SELECT p.product_id, s.supplier_id, (s.supplier_id=1+(p.product_id % 6))
FROM products p JOIN suppliers s
  ON s.supplier_id IN (1+(p.product_id % 6),1+((p.product_id+1) % 6));

INSERT INTO product_supplier_options
(product_supplier_id,name,base_factor,base_presentation,cost,avg_delivery_days,supplier_product_code,conversion_confirmed)
SELECT ps.product_supplier_id,'Unidade',1,TRUE,
       round(p.cost * CASE WHEN ps.preferred THEN 1.0 ELSE 1.08 END,2),
       s.avg_delivery_days,'FOR-' || lpad(ps.product_supplier_id::TEXT,5,'0'),TRUE
FROM product_suppliers ps JOIN products p ON p.product_id=ps.product_id
JOIN suppliers s ON s.supplier_id=ps.supplier_id;


INSERT INTO price_tables (name,active) VALUES
('Varejo',TRUE),('Oficinas',TRUE),('Atacado',TRUE);
INSERT INTO price_table_items (price_table_id,product_id,price)
SELECT pt.price_table_id,p.product_id,
       round(p.price * CASE pt.name WHEN 'Varejo' THEN 1.00 WHEN 'Oficinas' THEN 0.92 ELSE 0.85 END, 2)
FROM price_tables pt CROSS JOIN products p;


INSERT INTO customers (person_type,tax_id,legal_name,trade_name,email,phone,price_table_id) VALUES
('PJ','00000000000011','Oficina Piloto Motos Ltda','Piloto Motos','cliente1@example.invalid','81988880001',2),
('PJ','00000000000012','Moto Center Agreste Ltda','Moto Center','cliente2@example.invalid','81988880002',2),
('PJ','00000000000013','Oficina 2 Tempos Ltda','2 Tempos','cliente3@example.invalid','81988880003',2),
('PJ','00000000000014','Auto Pecas e Motos Brasil Ltda','Pecas Brasil','cliente4@example.invalid','81988880004',2),
('PJ','00000000000015','Speed Motos Comercio Ltda','Speed Motos','cliente5@example.invalid','81988880005',2),
('PJ','00000000000016','Oficina Sao Jorge Ltda','Sao Jorge Motos','cliente6@example.invalid','81988880006',2),
('PJ','00000000000017','Moto Pecas Sertao Ltda','Sertao Motos','cliente7@example.invalid','81988880007',2),
('PJ','00000000000018','Oficina Popular Ltda','Oficina Popular','cliente8@example.invalid','81988880008',2),
('PJ','00000000000019','Moto Shop PE Ltda','Moto Shop','cliente9@example.invalid','81988880009',2),
('PJ','00000000000020','Mecanica Duas Rodas Ltda','Mecanica 2 Rodas','cliente10@example.invalid','81988880010',2),
('PF','00000000021','Cliente Balcao 01',NULL,'cliente11@example.invalid','81988880011',1),
('PF','00000000022','Cliente Balcao 02',NULL,'cliente12@example.invalid','81988880012',1),
('PF','00000000023','Cliente Balcao 03',NULL,'cliente13@example.invalid','81988880013',1),
('PF','00000000024','Cliente Balcao 04',NULL,'cliente14@example.invalid','81988880014',1),
('PF','00000000025','Cliente Balcao 05',NULL,'cliente15@example.invalid','81988880015',1);


INSERT INTO customer_addresses (customer_id,state,city,street,neighborhood,number,complement,zip_code,address_type)
SELECT customer_id,'PE',CASE WHEN customer_id%2=0 THEN 'Caruaru' ELSE 'Bezerros' END,
       'Rua Cliente Ficticio','Centro',customer_id::TEXT,NULL,'55000-000','MAIN'
FROM customers;

INSERT INTO financial_categories (name,type) VALUES
('Venda de motopecas','REVENUE'),('Outras receitas','REVENUE'),
('Compra de mercadorias','EXPENSE'),('Frete','EXPENSE'),
('Despesa operacional','EXPENSE'),('Servicos e manutencao','EXPENSE');
INSERT INTO bank_accounts (name,type,initial_balance) VALUES
('Caixa Loja','CASH',1500),('Conta Corrente Demonstrativa','CHECKING_ACCOUNT',25000);

INSERT INTO companies (legal_name,trade_name,tax_id,tax_regime,nfe_environment,nfe_default_series,nfe_next_number)
VALUES ('Distribuidora Exemplo Motopecas Ltda','Exemplo Motopecas','00000000000099',
        'SIMPLES_NACIONAL','HOMOLOGATION','1',1);
INSERT INTO company_addresses (company_id,country,state,city,street,neighborhood,number,complement,zip_code)
SELECT company_id,'Brasil','PE','Caruaru','Rua Empresa Ficticia','Centro','500','Sede demonstrativa','55000-000' FROM companies;


DO $seed$
DECLARE
    v_order BIGINT; v_item BIGINT; v_stock BIGINT; v_prod BIGINT;
    v_user BIGINT; v_wh BIGINT; v_customer BIGINT;
    v_qty NUMERIC(14,1); v_price NUMERIC(12,2); v_total NUMERIC(12,2);
    v_status VARCHAR(20); v_order_time TIMESTAMP;
    v_receivable BIGINT;
BEGIN
    SELECT user_id INTO v_user FROM users WHERE email='operador-seed@example.invalid';
    SELECT warehouse_id INTO v_wh FROM warehouses WHERE name='CD Principal';
    FOR i IN 1..42 LOOP
        SELECT customer_id INTO v_customer FROM customers ORDER BY customer_id LIMIT 1 OFFSET ((i-1)%15);
        v_status := CASE WHEN i%7=0 THEN 'PENDING' ELSE 'CONFIRMED' END;
        v_order_time := CURRENT_TIMESTAMP - (42-i) * INTERVAL '18 hours';
        INSERT INTO orders (customer_id,user_id,status,payment_method,total_amount,created_at)
        VALUES (v_customer,v_user,v_status,
                CASE WHEN i%4=0 THEN 'BOLETO' WHEN i%4=1 THEN 'PIX'
                     WHEN i%4=2 THEN 'CREDIT_CARD' ELSE 'BANK_TRANSFER' END,0,v_order_time)
        RETURNING order_id INTO v_order;
        v_total := 0;
        FOR j IN 1..(1+i%3) LOOP
            SELECT p.product_id, p.price INTO v_prod,v_price FROM products p
            ORDER BY p.product_id LIMIT 1 OFFSET (((i*3+j*7)%32));
            v_qty := 1+(i+j)%3;
            SELECT stock_item_id INTO v_stock FROM stock_items
            WHERE product_id=v_prod AND warehouse_id=v_wh;
            INSERT INTO order_items (order_id,product_id,warehouse_id,base_quantity,
                  presentation_name_snapshot,base_factor_snapshot,unit_snapshot,
                  product_name_snapshot,sku_snapshot,quantity,unit_price,discount,discount_type)
            SELECT v_order,v_prod,v_wh,v_qty,'Unidade',1,'UN',pc.name,p.sku,v_qty,v_price,0,'AMOUNT'
            FROM products p JOIN product_catalogs pc ON pc.product_catalog_id=p.catalog_id
            WHERE p.product_id=v_prod
            RETURNING order_item_id INTO v_item;
            v_total := v_total+v_qty*v_price;
            IF v_status='CONFIRMED' THEN
                INSERT INTO stock_reservations (order_item_id,stock_item_id,quantity,status,created_at)
                VALUES (v_item,v_stock,v_qty,'ACTIVE',v_order_time);
                UPDATE stock_items SET reserved_quantity=reserved_quantity+v_qty WHERE stock_item_id=v_stock;
            END IF;
        END LOOP;
        UPDATE orders SET total_amount=v_total WHERE order_id=v_order;
        INSERT INTO accounts_receivable (customer_id,order_id,financial_category_id,
                  description,amount,due_date,status,issued_at)
        SELECT v_customer,v_order,financial_category_id,'Pedido '||v_order,v_total,
               (v_order_time::DATE+INTERVAL '15 days')::DATE,'OPEN',v_order_time
        FROM financial_categories WHERE name='Venda de motopecas'
        RETURNING account_receivable_id INTO v_receivable;
        -- Demonstracao de baixa parcial/integral de contas a receber.
        IF i%5=0 THEN
            INSERT INTO accounts_receivable_payments
            (account_receivable_id,bank_account_id,user_id,paid_amount,paid_at,payment_method,notes)
            SELECT v_receivable,bank_account_id,v_user,v_total,v_order_time::DATE,'PIX',
                   'Recebimento demonstrativo' FROM bank_accounts WHERE name='Conta Corrente Demonstrativa';
            UPDATE accounts_receivable SET status='PAID' WHERE account_receivable_id=v_receivable;
        ELSIF i%8=0 THEN
            INSERT INTO accounts_receivable_payments
            (account_receivable_id,bank_account_id,user_id,paid_amount,paid_at,payment_method,notes)
            SELECT v_receivable,bank_account_id,v_user,round(v_total/2,2),v_order_time::DATE,
                   'PIX','Recebimento parcial demonstrativo'
            FROM bank_accounts WHERE name='Conta Corrente Demonstrativa';
            UPDATE accounts_receivable SET status='PARTIAL' WHERE account_receivable_id=v_receivable;
        END IF;
    END LOOP;
END $seed$;


DO $seed$
DECLARE v_purchase BIGINT; v_user BIGINT; v_supp BIGINT; v_product BIGINT;
        v_wh BIGINT; v_option BIGINT; v_cost NUMERIC(12,2);
        v_qty NUMERIC(14,1); v_total NUMERIC(12,2);
        v_payable BIGINT;
BEGIN
    SELECT user_id INTO v_user FROM users WHERE email='operador-seed@example.invalid';
    SELECT warehouse_id INTO v_wh FROM warehouses WHERE name='CD Principal';
    FOR i IN 1..9 LOOP
        SELECT supplier_id INTO v_supp FROM suppliers ORDER BY supplier_id LIMIT 1 OFFSET ((i-1)%6);
        INSERT INTO purchases (supplier_id,user_id,status,expected_delivery_date,created_at)
        VALUES (v_supp,v_user,CASE WHEN i%3=0 THEN 'DRAFT' ELSE 'SENT' END,
                CURRENT_DATE+(5+i),CURRENT_TIMESTAMP-(i*INTERVAL '1 day'))
        RETURNING purchase_id INTO v_purchase;
        v_total:=0;
        FOR j IN 1..3 LOOP
            SELECT ps.product_id,pso.product_supplier_option_id,pso.cost
            INTO v_product,v_option,v_cost
            FROM product_suppliers ps JOIN product_supplier_options pso
              ON pso.product_supplier_id=ps.product_supplier_id
            WHERE ps.supplier_id=v_supp ORDER BY ps.product_id
            LIMIT 1 OFFSET ((i+j)%10);
            IF v_product IS NULL THEN CONTINUE; END IF;
            v_qty:=10+j*5;
            INSERT INTO purchase_items (purchase_id,product_id,warehouse_id,supplier_option_id,
                base_quantity,presentation_name_snapshot,base_factor_snapshot,unit_snapshot,
                product_name_snapshot,sku_snapshot,quantity,unit_cost,received_quantity)
            SELECT v_purchase,v_product,v_wh,v_option,v_qty,'Unidade',1,'UN',pc.name,p.sku,v_qty,v_cost,0
            FROM products p JOIN product_catalogs pc ON pc.product_catalog_id=p.catalog_id
            WHERE p.product_id=v_product;
            v_total:=v_total+v_qty*v_cost;
        END LOOP;
        UPDATE purchases SET total_amount=v_total WHERE purchase_id=v_purchase;
        INSERT INTO accounts_payable (supplier_id,purchase_id,financial_category_id,
                description,amount,due_date,status)
        SELECT v_supp,v_purchase,financial_category_id,'Compra '||v_purchase,
               v_total,CURRENT_DATE+(5+i),'OPEN'
        FROM financial_categories WHERE name='Compra de mercadorias'
        RETURNING account_payable_id INTO v_payable;
        IF i%4=0 THEN
            INSERT INTO accounts_payable_payments (account_payable_id,bank_account_id,user_id,
                          paid_amount,paid_at,payment_method,notes)
            SELECT v_payable,bank_account_id,v_user,v_total,CURRENT_DATE,'BANK_TRANSFER',
                   'Pagamento antecipado demonstrativo'
            FROM bank_accounts WHERE name='Conta Corrente Demonstrativa';
            UPDATE accounts_payable SET status='PAID' WHERE account_payable_id=v_payable;
        END IF;
    END LOOP;
END $seed$;


INSERT INTO purchase_suggestions (reference_period,status,reviewer_user_id)
SELECT to_char(CURRENT_DATE,'YYYY-MM'),'PENDING',user_id FROM users
WHERE email='operador-seed@example.invalid';

INSERT INTO purchase_suggestion_items (purchase_suggestion_id,product_id,supplier_id,
  period_sold_quantity,avg_daily_consumption,current_stock,min_stock,suggested_quantity)
SELECT sg.purchase_suggestion_id,p.product_id,ps.supplier_id,
       25+(p.product_id%14), round((25+(p.product_id%14))/30.0,2),
       si.quantity::INT,si.min_quantity::INT,
       GREATEST(0, 30+si.min_quantity::INT-si.quantity::INT)
FROM purchase_suggestions sg
JOIN products p ON p.product_id<=12
JOIN product_suppliers ps ON ps.product_id=p.product_id AND ps.preferred
JOIN stock_items si ON si.product_id=p.product_id
JOIN warehouses w ON w.warehouse_id=si.warehouse_id AND w.name='CD Principal';

-- NFe: somente documentos PENDING em ambiente de homologacao, sem chave nem protocolo.
INSERT INTO invoices (order_id,company_id,customer_id,number,series,operation_nature,
                      environment,status,products_total_amount,total_amount)
SELECT o.order_id,c.company_id,o.customer_id,
       row_number() OVER (ORDER BY o.order_id),'1',
       'Venda de mercadorias','HOMOLOGATION','PENDING',o.total_amount,o.total_amount
FROM orders o CROSS JOIN companies c
WHERE o.status='CONFIRMED' AND o.order_id IN
      (SELECT order_id FROM orders WHERE status='CONFIRMED' ORDER BY order_id LIMIT 4);

INSERT INTO invoice_items (invoice_id,product_id,quantity,unit_price,total_price,
                           ncm_snapshot,cfop_snapshot,cst_csosn_snapshot)
SELECT inv.invoice_id,oi.product_id,oi.base_quantity::INT,
       oi.unit_price,oi.base_quantity*oi.unit_price,p.ncm,NULL,NULL
FROM invoices inv JOIN order_items oi ON oi.order_id=inv.order_id
JOIN products p ON p.product_id=oi.product_id;

UPDATE companies SET nfe_next_number=(SELECT COALESCE(MAX(number),0)+1 FROM invoices)
WHERE nfe_environment='HOMOLOGATION';

-- A sequencia de SKU ja foi avancada via nextval(); nenhuma reinicializacao necessaria.
-- Nao inserir sessoes, convites, tokens, desafios ou eventos falsos de auditoria.

