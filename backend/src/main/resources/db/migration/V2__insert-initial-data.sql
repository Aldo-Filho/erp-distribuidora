-- Dados inteiramente novos e fictícios para desenvolvimento em banco novo.
-- Cada cenário cria um cadastro de cada tabela principal e suas relações.
-- 20 registros por tabela, exceto permissões, vínculos de perfil,
-- categorias financeiras e movimentações de estoque, que recebem 40.
-- Documentos e protocolos são ilustrativos. Nenhuma emissão fiscal é realizada.
-- Usuários ficam inativos e sem credenciais de login.

DO $$
DECLARE
    item RECORD;
    role_key BIGINT;
    read_permission_key BIGINT;
    write_permission_key BIGINT;
    user_key BIGINT;
    category_key BIGINT;
    brand_key BIGINT;
    product_key BIGINT;
    sku_number BIGINT;
    product_sku TEXT;
    warehouse_key BIGINT;
    stock_key BIGINT;
    supplier_key BIGINT;
    price_table_key BIGINT;
    customer_key BIGINT;
    suggestion_key BIGINT;
    purchase_key BIGINT;
    order_key BIGINT;
    revenue_key BIGINT;
    expense_key BIGINT;
    bank_key BIGINT;
    receivable_key BIGINT;
    payable_key BIGINT;
    company_key BIGINT;
    invoice_key BIGINT;
    purchase_quantity INTEGER;
    sale_quantity INTEGER;
    final_balance INTEGER;
    purchase_total NUMERIC(12,2);
    sale_total NUMERIC(12,2);
    payment_fraction NUMERIC(3,2);
    event_date DATE;
    person_type_value VARCHAR(2);
    customer_document VARCHAR(14);
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            (1, 'Café torrado tradicional 500 g', 'Cafés e chás', 'Serra Clara', 'Torrefação Horizonte', 'Marina Albuquerque', 'Campinas', 'SP', '19', 12.40, 19.90),
            (2, 'Arroz parboilizado 5 kg', 'Arroz e cereais', 'Campo Dourado', 'Cereais Rio Verde', 'Rafael Monteiro', 'Ribeirão Preto', 'SP', '16', 18.50, 28.90),
            (3, 'Feijão carioca 1 kg', 'Feijões e leguminosas', 'Terra Mansa', 'Grãos Santa Helena', 'Beatriz Azevedo', 'Uberlândia', 'MG', '34', 5.80, 8.90),
            (4, 'Macarrão espaguete 500 g', 'Massas secas', 'Vila Trigo', 'Massas Bela Mesa', 'Gustavo Farias', 'Juiz de Fora', 'MG', '32', 3.20, 5.49),
            (5, 'Óleo de girassol 900 ml', 'Óleos e azeites', 'Sol do Vale', 'Óleos Primavera', 'Camila Nogueira', 'Londrina', 'PR', '43', 6.70, 10.90),
            (6, 'Açúcar demerara 1 kg', 'Açúcares e adoçantes', 'Doce Estação', 'Engenho Aurora', 'Lucas Peixoto', 'Maringá', 'PR', '44', 4.10, 6.99),
            (7, 'Farinha de mandioca 1 kg', 'Farinhas e féculas', 'Raiz Serena', 'Alimentos Raiz do Campo', 'Juliana Vasconcelos', 'Joinville', 'SC', '47', 4.60, 7.90),
            (8, 'Biscoito integral 400 g', 'Biscoitos e snacks', 'Forno Alegre', 'Biscoitos Boa Fornada', 'Henrique Barros', 'Blumenau', 'SC', '47', 5.20, 8.49),
            (9, 'Molho de tomate 300 g', 'Molhos e temperos', 'Horta Viva', 'Conservas Jardim Sul', 'Fernanda Siqueira', 'Caxias do Sul', 'RS', '54', 1.90, 3.29),
            (10, 'Leite em pó integral 400 g', 'Leites e derivados', 'Pastos Altos', 'Laticínios Pedra Branca', 'Tiago Medeiros', 'Pelotas', 'RS', '53', 10.30, 16.90),
            (11, 'Suco de uva integral 1 l', 'Sucos e néctares', 'Fruta Nobre', 'Bebidas Vale da Uva', 'Empório Ponto Certo', 'Sorocaba', 'SP', '15', 8.90, 14.90),
            (12, 'Água mineral sem gás 1,5 l', 'Águas minerais', 'Fonte Cristal', 'Águas Serra Azul', 'Mercadinho Vila Nova', 'Bauru', 'SP', '14', 1.60, 2.99),
            (13, 'Sabão em pó 800 g', 'Lavanderia', 'Brilho Leve', 'Química Limpa Casa', 'Comercial Flor do Bairro', 'Anápolis', 'GO', '62', 6.90, 11.90),
            (14, 'Detergente neutro 500 ml', 'Limpeza de cozinha', 'Prato Limpo', 'Higiene Água Clara', 'Armazém Bom Vizinho', 'Dourados', 'MS', '67', 1.25, 2.39),
            (15, 'Desinfetante lavanda 2 l', 'Limpeza de ambientes', 'Casa Fresca', 'Saneantes Porto Limpo', 'Mercearia Três Irmãos', 'Rondonópolis', 'MT', '66', 4.80, 8.49),
            (16, 'Papel higiênico 12 rolos', 'Papéis sanitários', 'Toque Suave', 'Papéis Lago Branco', 'Loja Utilidades Sol Nascente', 'Feira de Santana', 'BA', '75', 8.30, 13.90),
            (17, 'Sabonete vegetal 90 g', 'Higiene pessoal', 'Pele Serena', 'Cosméticos Flor Nativa', 'Conveniência Esquina Azul', 'Campina Grande', 'PB', '83', 1.80, 3.49),
            (18, 'Creme dental menta 90 g', 'Cuidados bucais', 'Sorriso Vivo', 'Higiene Oral Novo Dia', 'Supermercado Canto Verde', 'Mossoró', 'RN', '84', 2.90, 5.29),
            (19, 'Ração para cães adultos 3 kg', 'Alimentação animal', 'Amigo Fiel', 'Nutrição Animal Campo Bom', 'Pet Shop Patas e Companhia', 'Sobral', 'CE', '88', 19.70, 32.90),
            (20, 'Saco de lixo reforçado 50 l', 'Descartáveis domésticos', 'Eco Prático', 'Embalagens Novo Ciclo', 'Distribuidora Bairro Unido', 'Caruaru', 'PE', '81', 3.60, 6.49)
        ) AS data(n, product_name, category_name, brand_name, supplier_name,
            customer_name, city, state, area_code, cost, price)
    LOOP
        sku_number := nextval('product_sku_seq');
        product_sku := 'PRD-' || LPAD(sku_number::TEXT, GREATEST(6, LENGTH(sku_number::TEXT)), '0');

        event_date := DATE '2026-09-01' + item.n;
        purchase_quantity := 20 + item.n;
        sale_quantity := 1 + item.n % 5;
        final_balance := 80 + item.n * 3;
        purchase_total := item.cost * purchase_quantity;
        sale_total := item.price * sale_quantity;
        payment_fraction := CASE WHEN item.n % 3 = 0 THEN 0.50 ELSE 1.00 END;
        person_type_value := CASE WHEN item.n <= 10 THEN 'PF' ELSE 'PJ' END;
        customer_document := CASE WHEN item.n <= 10
            THEN '710000000' || LPAD(item.n::TEXT, 2, '0')
            ELSE '7200000000' || LPAD(item.n::TEXT, 4, '0') END;

        -- Acesso: perfil e operador próprios de cada centro regional.
        INSERT INTO roles (name, description, created_at, active)
        VALUES ('Operação ' || item.city, 'Perfil regional fictício de distribuição', TIMESTAMP '2026-08-01 08:00:00', TRUE)
        RETURNING role_id INTO role_key;

        INSERT INTO permissions (module, action, permission_key)
        VALUES ('regional_distribution', 'read', 'distribution.' || LOWER(product_sku) || '.read')
        RETURNING permission_id INTO read_permission_key;
        INSERT INTO permissions (module, action, permission_key)
        VALUES ('regional_distribution', 'write', 'distribution.' || LOWER(product_sku) || '.write')
        RETURNING permission_id INTO write_permission_key;

        INSERT INTO role_permissions (role_id, permission_id)
        VALUES (role_key, read_permission_key), (role_key, write_permission_key);

        INSERT INTO users (name, email, password_hash, created_at, role_id, active)
        VALUES ('Operador Regional ' || item.city, 'operacao.' || item.n || '@example.com',
            'SEED_ONLY_NO_LOGIN', TIMESTAMP '2026-08-01 08:00:00', role_key, FALSE)
        RETURNING user_id INTO user_key;

        INSERT INTO categories (name) VALUES (item.category_name) RETURNING category_id INTO category_key;
        INSERT INTO brands (name) VALUES (item.brand_name) RETURNING brand_id INTO brand_key;

        INSERT INTO products (
            name, sku, category_id, brand_id, cost, price, weight_kg,
            commercial_unit, origin, active, created_at
        ) VALUES (item.product_name, product_sku, category_key, brand_key, item.cost, item.price,
            0.100 + item.n * 0.050, 'UN', 'NATIONAL', item.n % 7 <> 0, TIMESTAMP '2026-08-01 08:00:00')
        RETURNING product_id INTO product_key;

        INSERT INTO warehouses (name, description)
        VALUES ('Centro Logístico ' || item.city, 'Unidade regional de recebimento e expedição em ' || item.state)
        RETURNING warehouse_id INTO warehouse_key;

        INSERT INTO warehouse_addresses (
            warehouse_id, state, city, street, neighborhood, number, complement, zip_code
        ) VALUES (warehouse_key, item.state, item.city, 'Avenida das Indústrias', 'Distrito Industrial',
            (100 + item.n)::TEXT, 'Galpão de distribuição', '00000-000');

        INSERT INTO stock_items (
            warehouse_id, product_id, quantity, reserved_quantity, min_quantity, max_quantity
        ) VALUES (warehouse_key, product_key, final_balance, item.n % 7, 10 + item.n, 250 + item.n)
        RETURNING stock_item_id INTO stock_key;

        INSERT INTO suppliers (
            legal_name, trade_name, tax_id, email, phone, whatsapp, avg_delivery_days, active, created_at
        ) VALUES (item.supplier_name || ' Ltda.', item.supplier_name,
            '7300000000' || LPAD(item.n::TEXT, 4, '0'),
            CASE WHEN item.n % 4 = 0 THEN NULL ELSE 'comercial.' || item.n || '@example.com' END,
            item.area_code || '3000' || LPAD(item.n::TEXT, 4, '0'),
            CASE WHEN item.n % 3 = 0 THEN NULL ELSE item.area_code || '99000' || LPAD(item.n::TEXT, 4, '0') END,
            CASE WHEN item.n % 6 = 0 THEN NULL ELSE 1 + item.n % 18 END,
            item.n % 6 <> 0, TIMESTAMP '2026-08-01 08:00:00')
        RETURNING supplier_id INTO supplier_key;

        INSERT INTO supplier_addresses (
            supplier_id, country, state, city, street, neighborhood, number, complement, zip_code, address_type
        ) VALUES (supplier_key, 'Brasil', item.state, item.city, 'Rua dos Fabricantes', 'Distrito Industrial',
            (200 + item.n)::TEXT, CASE WHEN item.n % 2 = 0 THEN NULL ELSE 'Bloco comercial' END,
            '00000-000', 'MAIN');

        INSERT INTO product_suppliers (
            product_id, supplier_id, cost, avg_delivery_days, supplier_product_code, preferred, active
        ) VALUES (product_key, supplier_key, item.cost, 1 + item.n % 18,
            'FAB-' || product_sku, item.n % 3 <> 0, item.n % 6 <> 0 AND item.n % 7 <> 0);
        INSERT INTO supplier_categories (supplier_id, category_id) VALUES (supplier_key, category_key);
        INSERT INTO supplier_brands (supplier_id, brand_id) VALUES (supplier_key, brand_key);

        INSERT INTO price_tables (name, start_date, end_date, active, created_at)
        VALUES ('Condição Comercial ' || item.city, DATE '2026-08-01', DATE '2027-07-31',
            item.n % 8 <> 0, TIMESTAMP '2026-08-01 08:00:00')
        RETURNING price_table_id INTO price_table_key;
        INSERT INTO price_table_items (price_table_id, product_id, price)
        VALUES (price_table_key, product_key, item.price);

        INSERT INTO customers (
            person_type, tax_id, legal_name, trade_name, birth_date, email,
            phone, whatsapp, active, price_table_id, created_at
        ) VALUES (person_type_value, customer_document,
            item.customer_name || CASE WHEN person_type_value = 'PJ' THEN ' Ltda.' ELSE '' END,
            CASE WHEN person_type_value = 'PJ' THEN item.customer_name ELSE NULL END,
            CASE WHEN person_type_value = 'PF' THEN DATE '1980-04-15' + item.n * 470 ELSE NULL END,
            CASE WHEN item.n % 4 = 0 THEN NULL ELSE 'cliente.regional.' || item.n || '@example.com' END,
            item.area_code || '98000' || LPAD(item.n::TEXT, 4, '0'),
            CASE WHEN item.n % 3 = 0 THEN NULL ELSE item.area_code || '98000' || LPAD(item.n::TEXT, 4, '0') END,
            item.n % 9 <> 0, price_table_key, TIMESTAMP '2026-08-01 08:00:00')
        RETURNING customer_id INTO customer_key;

        INSERT INTO customer_addresses (
            customer_id, state, city, street, neighborhood, number, complement, zip_code, address_type
        ) VALUES (customer_key, item.state, item.city, 'Rua do Comércio', 'Centro',
            (300 + item.n)::TEXT, CASE WHEN person_type_value = 'PJ' THEN 'Loja térrea' ELSE 'Casa' END,
            '00000-000', 'MAIN');

        INSERT INTO financial_categories (name, type, active)
        VALUES ('Receita regional - ' || item.category_name, 'REVENUE', TRUE)
        RETURNING financial_category_id INTO revenue_key;
        INSERT INTO financial_categories (name, type, active)
        VALUES ('Aquisição regional - ' || item.category_name, 'EXPENSE', TRUE)
        RETURNING financial_category_id INTO expense_key;

        INSERT INTO bank_accounts (name, type, initial_balance, active)
        VALUES ('Conta Regional ' || item.city,
            CASE WHEN item.n % 3 = 0 THEN 'CASH' WHEN item.n % 3 = 1 THEN 'CHECKING_ACCOUNT' ELSE 'SAVINGS_ACCOUNT' END,
            10000.00 + item.n * 500.00, TRUE)
        RETURNING bank_account_id INTO bank_key;

        INSERT INTO companies (
            legal_name, trade_name, tax_id, tax_regime, nfe_environment,
            nfe_default_series, nfe_next_number
        ) VALUES ('Distribuidora ' || item.brand_name || ' Regional Ltda.',
            'Distribuidora ' || item.brand_name, '7400000000' || LPAD(item.n::TEXT, 4, '0'),
            'SIMPLES_NACIONAL', 'HOMOLOGATION', '1', 2)
        RETURNING company_id INTO company_key;

        INSERT INTO company_addresses (
            company_id, country, state, city, street, neighborhood, number, complement, zip_code
        ) VALUES (company_key, 'Brasil', item.state, item.city, 'Avenida Regional', 'Centro',
            (400 + item.n)::TEXT, 'Sede administrativa', '00000-000');

        INSERT INTO purchase_suggestions (generated_at, updated_at, reference_period, status, reviewer_user_id)
        VALUES (event_date - 3, event_date - 2, '2026-08', 'APPROVED', user_key)
        RETURNING purchase_suggestion_id INTO suggestion_key;

        INSERT INTO purchase_suggestion_items (
            purchase_suggestion_id, product_id, supplier_id, period_sold_quantity,
            avg_daily_consumption, current_stock, min_stock, suggested_quantity
        ) VALUES (suggestion_key, product_key, supplier_key, 30 + item.n,
            ROUND((30.00 + item.n) / 30.00, 2), final_balance + sale_quantity - purchase_quantity,
            10 + item.n, purchase_quantity);

        INSERT INTO purchases (
            supplier_id, user_id, purchase_suggestion_id, status, created_at,
            updated_at, expected_delivery_date, total_amount
        ) VALUES (supplier_key, user_key, suggestion_key, 'RECEIVED', event_date - 2,
            event_date, event_date, purchase_total)
        RETURNING purchase_id INTO purchase_key;

        INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, received_quantity)
        VALUES (purchase_key, product_key, purchase_quantity, item.cost, purchase_quantity);

        INSERT INTO orders (total_amount, payment_method, status, created_at, customer_id, user_id)
        VALUES (sale_total, CASE WHEN item.n % 2 = 0 THEN 'BOLETO' ELSE 'PIX' END,
            'INVOICED', event_date + 1, customer_key, user_key)
        RETURNING order_id INTO order_key;
        INSERT INTO order_items (order_id, product_id, quantity, unit_price, discount)
        VALUES (order_key, product_key, sale_quantity, item.price, 0.00);

        -- Entrada e saída terminam exatamente no saldo cadastrado em stock_items.
        INSERT INTO stock_movements (
            stock_item_id, type, reason, quantity, previous_balance, new_balance,
            purchase_id, user_id, occurred_at, notes
        ) VALUES (stock_key, 'INBOUND', 'PURCHASE', purchase_quantity,
            final_balance + sale_quantity - purchase_quantity, final_balance + sale_quantity,
            purchase_key, user_key, event_date, 'Recebimento regional de ' || item.product_name);
        INSERT INTO stock_movements (
            stock_item_id, type, reason, quantity, previous_balance, new_balance,
            order_id, user_id, occurred_at, notes
        ) VALUES (stock_key, 'OUTBOUND', 'SALE', sale_quantity,
            final_balance + sale_quantity, final_balance, order_key, user_key,
            event_date + 1, 'Expedição para ' || item.customer_name);

        INSERT INTO accounts_receivable (
            customer_id, order_id, financial_category_id, description, amount, due_date, status, issued_at
        ) VALUES (customer_key, order_key, revenue_key, 'Venda regional - ' || item.product_name,
            sale_total, event_date + 8, CASE WHEN payment_fraction = 1.00 THEN 'PAID' ELSE 'PARTIAL' END, event_date + 1)
        RETURNING account_receivable_id INTO receivable_key;
        INSERT INTO accounts_receivable_payments (
            account_receivable_id, bank_account_id, user_id, paid_amount, paid_at, payment_method, notes
        ) VALUES (receivable_key, bank_key, user_key, ROUND(sale_total * payment_fraction, 2),
            event_date + 2, CASE WHEN item.n % 2 = 0 THEN 'BOLETO' ELSE 'PIX' END, 'Recebimento fictício regional');

        INSERT INTO accounts_payable (
            supplier_id, purchase_id, financial_category_id, description, amount, due_date, status, issued_at
        ) VALUES (supplier_key, purchase_key, expense_key, 'Aquisição regional - ' || item.product_name,
            purchase_total, event_date + 10, CASE WHEN payment_fraction = 1.00 THEN 'PAID' ELSE 'PARTIAL' END, event_date)
        RETURNING account_payable_id INTO payable_key;
        INSERT INTO accounts_payable_payments (
            account_payable_id, bank_account_id, user_id, paid_amount, paid_at, payment_method, notes
        ) VALUES (payable_key, bank_key, user_key, ROUND(purchase_total * payment_fraction, 2),
            event_date + 2, 'BANK_TRANSFER', 'Pagamento fictício ao fabricante regional');

        -- Documentos de homologação ilustrativos, sem XML ou chave de acesso reais.
        INSERT INTO invoices (
            order_id, company_id, customer_id, number, series, operation_nature,
            purpose, environment, status, authorization_protocol, issued_at,
            authorized_at, products_total_amount, total_amount
        ) VALUES (order_key, company_key, customer_key, 1, '1', 'Venda de mercadorias regionais',
            'NORMAL', 'HOMOLOGATION', 'AUTHORIZED', 'SIMULADO-REG-' || LPAD(item.n::TEXT, 2, '0'),
            event_date + 1, event_date + 1, sale_total, sale_total)
        RETURNING invoice_id INTO invoice_key;
        INSERT INTO invoice_items (
            invoice_id, product_id, quantity, unit_price, total_price,
            cfop_snapshot, cst_csosn_snapshot, icms_rate_snapshot, icms_amount,
            ipi_rate_snapshot, ipi_amount, pis_rate_snapshot, pis_amount,
            cofins_rate_snapshot, cofins_amount
        ) VALUES (invoice_key, product_key, sale_quantity, item.price, sale_total,
            '5102', '102', 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00);
        INSERT INTO invoice_events (invoice_id, event_type, occurred_at, protocol, description)
        VALUES (invoice_key, 'CORRECTION_LETTER', event_date + 2,
            'SIMULADO-CC-' || LPAD(item.n::TEXT, 2, '0'), 'Correção ilustrativa da descrição de entrega regional');
    END LOOP;
END;
$$;
