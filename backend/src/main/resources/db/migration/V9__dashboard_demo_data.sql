-- DEMONSTRACAO: este arquivo INSERE dados ficticios no banco configurado no Flyway.
-- Executar somente em uma base destinada a demonstracao/desenvolvimento.
-- Aplicar migrations posteriores a V8 ao iniciar o backend tambem executara esta V9.
-- Nenhum cadastro existente e atualizado; IDs sao obtidos por RETURNING.
-- Nao cria credenciais utilizaveis, concede permissoes ou altera preferencias.
-- Datas relativas ao dia de APLICACAO (America/Sao_Paulo); nao se renovam a cada acesso.
-- Confirmacoes sao eventos destes NOVOS exemplos, nunca backfill de vendas reais.

DO $dashboard_demo$
DECLARE
    demo_today DATE := (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date;
    demo_role BIGINT;
    demo_user BIGINT;
    demo_brand BIGINT;
    demo_category BIGINT;
    demo_customer BIGINT;
    demo_supplier BIGINT;
    demo_bank BIGINT;
    demo_revenue BIGINT;
    demo_expense BIGINT;
    demo_catalog BIGINT;
    demo_product BIGINT;
    demo_link BIGINT;
    demo_stock BIGINT;
    demo_order BIGINT;
    demo_item BIGINT;
    demo_reservation BIGINT;
    demo_title BIGINT;
    demo_purchase BIGINT;
    demo_suggestion BIGINT;
    demo_products BIGINT[] := ARRAY[]::BIGINT[];
    demo_presentations BIGINT[] := ARRAY[]::BIGINT[];
    demo_options BIGINT[] := ARRAY[]::BIGINT[];
    demo_warehouses BIGINT[] := ARRAY[]::BIGINT[];
    demo_key BIGINT;
    demo_n INTEGER;
    demo_day INTEGER;
    demo_sale INTEGER;
    demo_line INTEGER;
    demo_index INTEGER;
    demo_depot INTEGER;
    demo_digit INTEGER;
    demo_sum INTEGER;
    demo_pos INTEGER;
    demo_counter INTEGER := 0;
    demo_document TEXT;
    demo_cpf TEXT;
    demo_cnpj TEXT;
    demo_unit TEXT;
    demo_status TEXT;
    demo_name TEXT;
    demo_sku TEXT;
    demo_date DATE;
    demo_due DATE;
    demo_paid BOOLEAN;
    demo_factor NUMERIC;
    demo_quantity NUMERIC;
    demo_base NUMERIC;
    demo_price NUMERIC;
    demo_discount NUMERIC;
    demo_discount_type TEXT;
    demo_total NUMERIC;
    demo_before NUMERIC;
    demo_after NUMERIC;
    demo_reserved NUMERIC;
    demo_sold NUMERIC;
    demo_target NUMERIC;
    demo_shortage NUMERIC;
    demo_available NUMERIC;
    demo_names TEXT[] := ARRAY['Cafe tradicional 500 g','Arroz 5 kg','Feijao 1 kg',
        'Macarrao 500 g','Oleo 900 ml','Acucar 1 kg','Castanha a granel','Granola a granel'];
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE lower(btrim(email))='dashboard.demo@example.invalid') THEN
        RAISE EXCEPTION 'Identificador DEMO Dashboard ja existe; nenhum dado foi substituido.';
    END IF;

    -- Documentos sinteticos com digitos verificadores; nao representam pessoas/empresas reais.
    -- Em caso de colisao, gerar outro documento em vez de reutilizar um cliente/fornecedor real.
    LOOP
        demo_document := (910000000 + demo_counter)::text;
        FOR demo_n IN 1..2 LOOP
            demo_sum := 0;
            FOR demo_pos IN 1..length(demo_document) LOOP
                demo_sum := demo_sum + substring(demo_document,demo_pos,1)::integer * (length(demo_document)+2-demo_pos);
            END LOOP;
            demo_digit := CASE WHEN demo_sum % 11 < 2 THEN 0 ELSE 11 - demo_sum % 11 END;
            demo_document := demo_document || demo_digit;
        END LOOP;
        EXIT WHEN NOT EXISTS (SELECT 1 FROM customers WHERE upper(regexp_replace(tax_id,'[^0-9A-Za-z]','','g'))=demo_document);
        demo_counter := demo_counter + 1;
    END LOOP;
    demo_cpf := demo_document;
    demo_counter := 0;
    LOOP
        demo_document := (91000000 + demo_counter)::text || '0001';
        FOR demo_n IN 1..2 LOOP
            demo_sum := 0;
            FOR demo_pos IN 1..length(demo_document) LOOP
                demo_sum := demo_sum + substring(demo_document,demo_pos,1)::integer * (2 + (length(demo_document)-demo_pos) % 8);
            END LOOP;
            demo_digit := CASE WHEN demo_sum % 11 < 2 THEN 0 ELSE 11 - demo_sum % 11 END;
            demo_document := demo_document || demo_digit;
        END LOOP;
        EXIT WHEN NOT EXISTS (SELECT 1 FROM suppliers WHERE regexp_replace(tax_id,'[^0-9]','','g')=demo_document);
        demo_counter := demo_counter + 1;
    END LOOP;
    demo_cnpj := demo_document;

    INSERT INTO roles(name,description,active)
        VALUES ('DEMO Dashboard - operador sem acesso','Identifica eventos ficticios da migration V9; sem permissoes',FALSE)
        RETURNING role_id INTO demo_role;
    INSERT INTO users(name,email,password_hash,role_id,active)
        VALUES ('DEMO Dashboard - operador ficticio','dashboard.demo@example.invalid','SEED_ONLY_NO_LOGIN',demo_role,FALSE)
        RETURNING user_id INTO demo_user;
    INSERT INTO brands(name) VALUES ('DEMO Dashboard - marca') RETURNING brand_id INTO demo_brand;
    INSERT INTO categories(name) VALUES ('DEMO Dashboard - produtos') RETURNING category_id INTO demo_category;
    INSERT INTO customers(person_type,tax_id,legal_name,email)
        VALUES ('PF',demo_cpf,'DEMO Dashboard - cliente ficticio','cliente.dashboard@example.invalid') RETURNING customer_id INTO demo_customer;
    INSERT INTO suppliers(tax_id,legal_name,trade_name,email,avg_delivery_days)
        VALUES (demo_cnpj,'DEMO Dashboard - fornecedor ficticio','DEMO Dashboard','fornecedor.dashboard@example.invalid',7) RETURNING supplier_id INTO demo_supplier;
    INSERT INTO bank_accounts(name,type,initial_balance)
        VALUES ('DEMO Dashboard - caixa ficticio','CASH',0) RETURNING bank_account_id INTO demo_bank;
    INSERT INTO financial_categories(name,type)
        VALUES ('DEMO Dashboard - vendas','REVENUE') RETURNING financial_category_id INTO demo_revenue;
    INSERT INTO financial_categories(name,type)
        VALUES ('DEMO Dashboard - compras','EXPENSE') RETURNING financial_category_id INTO demo_expense;
    FOR demo_n IN 1..2 LOOP
        INSERT INTO warehouses(name,description) VALUES ('DEMO Dashboard - deposito '||demo_n,'Estoque ficticio para demonstracao V9')
            RETURNING warehouse_id INTO demo_key;
        demo_warehouses := array_append(demo_warehouses,demo_key);
    END LOOP;

    FOR demo_n IN 1..8 LOOP
        demo_unit := CASE WHEN demo_n<=6 THEN 'UN' ELSE 'KG' END;
        demo_price := 5.9 + demo_n*3.1;
        INSERT INTO product_catalogs(name,brand_id,category_id,unit,type)
            VALUES ('DEMO Dashboard - '||demo_names[demo_n],demo_brand,demo_category,demo_unit,'SIMPLE') RETURNING product_catalog_id INTO demo_catalog;
        demo_key := nextval('product_sku_seq');
        demo_sku := 'PRD-'||lpad(demo_key::text,greatest(6,length(demo_key::text)),'0');
        INSERT INTO products(catalog_id,sku,cost,price)
            VALUES (demo_catalog,demo_sku,round(demo_price*0.6,2),demo_price) RETURNING product_id INTO demo_product;
        demo_products := array_append(demo_products,demo_product);
        INSERT INTO product_presentations(product_id,name,base_factor,base_presentation)
            VALUES (demo_product,'BASE',1,TRUE) RETURNING product_presentation_id INTO demo_key;
        INSERT INTO product_presentations(product_id,name,base_factor,base_presentation,price)
            VALUES (demo_product,CASE WHEN demo_unit='UN' THEN 'Caixa com 6 unidades' ELSE 'Saco com 6 kg' END,6,FALSE,round(demo_price*6,2)) RETURNING product_presentation_id INTO demo_key;
        demo_presentations := array_append(demo_presentations,demo_key);
        INSERT INTO product_suppliers(product_id,supplier_id,preferred)
            VALUES (demo_product,demo_supplier,TRUE) RETURNING product_supplier_id INTO demo_link;
        INSERT INTO product_supplier_options(product_supplier_id,name,base_factor,base_presentation,cost,avg_delivery_days,conversion_confirmed)
            VALUES (demo_link,'BASE',1,TRUE,round(demo_price*0.6,2),7,TRUE) RETURNING product_supplier_option_id INTO demo_key;
        demo_options := array_append(demo_options,demo_key);
        FOR demo_depot IN 1..2 LOOP
            INSERT INTO stock_items(warehouse_id,product_id,quantity,reserved_quantity,min_quantity)
                VALUES (demo_warehouses[demo_depot],demo_product,10000,0,30) RETURNING stock_item_id INTO demo_stock;
            INSERT INTO stock_movements(stock_item_id,type,reason,quantity,previous_balance,new_balance,user_id,occurred_at,notes)
                VALUES (demo_stock,'INBOUND','MANUAL_ADJUSTMENT',10000,0,10000,demo_user,(demo_today-122)+TIME '08:00','DEMO Dashboard - abertura ficticia');
        END LOOP;
    END LOOP;

    -- 240 vendas / 480 itens: periodo atual e anterior com variacao deterministica.
    -- Confirmadas reservam estoque; despachadas possuem reserva consumida e movimento.
    FOR demo_day IN REVERSE 119..0 LOOP
        demo_date := demo_today-demo_day;
        FOR demo_sale IN 1..2 LOOP
            demo_n := demo_day*2+demo_sale;
            demo_status := CASE WHEN demo_n%19=0 THEN 'CANCELED' WHEN demo_n%17=0 THEN 'PENDING'
                WHEN demo_n%4=0 THEN 'CONFIRMED' ELSE 'INVOICED' END;
            demo_due := CASE WHEN demo_day<8 THEN demo_today+greatest(1,demo_day%7) ELSE demo_date+7 END;
            demo_paid := demo_status='INVOICED' OR (demo_status='CONFIRMED' AND demo_n%8=0);
            INSERT INTO orders(customer_id,user_id,status,payment_method,payment_due_date,total_amount,created_at,confirmed_at,updated_at)
                VALUES (demo_customer,demo_user,demo_status,'BOLETO',demo_due,0,(demo_date-2)+TIME '09:00',
                    CASE WHEN demo_status='PENDING' THEN NULL ELSE demo_date+TIME '10:00' END,demo_date+TIME '16:00') RETURNING order_id INTO demo_order;
            demo_total := 0;
            FOR demo_line IN 1..2 LOOP
                demo_index := 1+(demo_n+demo_line*3)%8;
                demo_depot := 1+(demo_n+demo_line)%2;
                demo_unit := CASE WHEN demo_index<=6 THEN 'UN' ELSE 'KG' END;
                demo_factor := CASE WHEN demo_n%5=0 THEN 6 ELSE 1 END;
                demo_quantity := CASE WHEN demo_unit='KG' AND demo_factor=1 THEN (1+demo_n%7)*0.5 ELSE 1+demo_n%5 END;
                demo_base := demo_quantity*demo_factor;
                demo_price := round((5.9+demo_index*3.1)*demo_factor*(1+(demo_day%9)*0.01),2);
                demo_discount_type := CASE WHEN demo_n%2=0 THEN 'PERCENTAGE' ELSE 'AMOUNT' END;
                demo_discount := CASE WHEN demo_discount_type='PERCENTAGE' THEN 5 ELSE 1 END;
                SELECT c.name,p.sku INTO demo_name,demo_sku FROM products p JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id WHERE p.product_id=demo_products[demo_index];
                INSERT INTO order_items(order_id,product_id,warehouse_id,presentation_id,base_quantity,presentation_name_snapshot,base_factor_snapshot,unit_snapshot,product_name_snapshot,sku_snapshot,attributes_snapshot,quantity,unit_price,discount,discount_type)
                    VALUES (demo_order,demo_products[demo_index],demo_warehouses[demo_depot],CASE WHEN demo_factor=6 THEN demo_presentations[demo_index] ELSE NULL END,demo_base,
                        CASE WHEN demo_factor=6 THEN 'Pacote de 6 '||demo_unit ELSE 'BASE' END,demo_factor,demo_unit,demo_name,demo_sku,'{}',demo_quantity,demo_price,demo_discount,demo_discount_type)
                    RETURNING order_item_id INTO demo_item;
                demo_total := demo_total+round(demo_quantity*demo_price,2)-CASE WHEN demo_discount_type='PERCENTAGE' THEN round(round(demo_quantity*demo_price,2)*demo_discount/100,2) ELSE demo_discount END;
                IF demo_status IN ('CONFIRMED','INVOICED','CANCELED') THEN
                    SELECT stock_item_id,quantity INTO demo_stock,demo_before FROM stock_items WHERE product_id=demo_products[demo_index] AND warehouse_id=demo_warehouses[demo_depot];
                    INSERT INTO stock_reservations(order_item_id,stock_item_id,quantity,status,created_at,updated_at)
                        VALUES (demo_item,demo_stock,demo_base,CASE WHEN demo_status='CONFIRMED' THEN 'ACTIVE' WHEN demo_status='CANCELED' THEN 'RELEASED' ELSE 'CONSUMED' END,demo_date+TIME '10:00',demo_date+TIME '16:00') RETURNING stock_reservation_id INTO demo_reservation;
                    IF demo_status='CONFIRMED' THEN
                        UPDATE stock_items SET reserved_quantity=reserved_quantity+demo_base WHERE stock_item_id=demo_stock;
                    ELSIF demo_status='INVOICED' THEN
                        UPDATE stock_items SET quantity=quantity-demo_base WHERE stock_item_id=demo_stock;
                        INSERT INTO stock_movements(stock_item_id,type,reason,quantity,previous_balance,new_balance,order_id,user_id,occurred_at,notes,stock_reservation_id)
                            VALUES (demo_stock,'OUTBOUND','SALE',demo_base,demo_before,demo_before-demo_base,demo_order,demo_user,demo_date+TIME '16:00','DEMO Dashboard - despacho ficticio',demo_reservation);
                    END IF;
                END IF;
            END LOOP;
            UPDATE orders SET total_amount=demo_total WHERE order_id=demo_order;
            IF demo_status<>'PENDING' THEN
                INSERT INTO accounts_receivable(customer_id,order_id,financial_category_id,description,amount,due_date,status,issued_at,payment_method,legacy,created_by,updated_by)
                    VALUES (demo_customer,demo_order,demo_revenue,'DEMO Dashboard - venda ficticia',demo_total,demo_due,
                        CASE WHEN demo_status='CANCELED' THEN 'CANCELED' WHEN demo_paid THEN 'PAID' ELSE 'OPEN' END,demo_date+TIME '10:00','BOLETO',FALSE,demo_user,demo_user)
                    RETURNING account_receivable_id INTO demo_title;
                IF demo_paid THEN
                    INSERT INTO accounts_receivable_payments(account_receivable_id,bank_account_id,user_id,paid_amount,paid_at,payment_method,notes,legacy,recorded_at)
                        VALUES (demo_title,demo_bank,demo_user,demo_total,demo_date,'BOLETO','DEMO Dashboard - recebimento ficticio',FALSE,demo_date+TIME '11:00');
                END IF;
            END IF;
        END LOOP;
    END LOOP;

    -- 30 compras sem recebimento; nao inventar estoque recebido a partir de compromissos.
    FOR demo_n IN 0..29 LOOP
        demo_date := demo_today-demo_n*4;
        demo_status := CASE WHEN demo_n%7=0 THEN 'DRAFT' WHEN demo_n%7=1 THEN 'SENT' WHEN demo_n%7=2 THEN 'CANCELED' ELSE 'CONFIRMED' END;
        demo_due := CASE WHEN demo_n<7 THEN demo_today+demo_n%7 ELSE demo_date+14 END;
        demo_paid := demo_status='CONFIRMED' AND demo_n%2=0;
        INSERT INTO purchases(supplier_id,user_id,status,created_at,expected_delivery_date,total_amount)
            VALUES (demo_supplier,demo_user,demo_status,demo_date+TIME '08:00',demo_due,0) RETURNING purchase_id INTO demo_purchase;
        demo_total := 0;
        FOR demo_line IN 1..2 LOOP
            demo_index := 1+(demo_n+demo_line)%8;
            demo_quantity := 20+demo_n*2+demo_line;
            demo_price := round((5.9+demo_index*3.1)*0.6,2);
            SELECT c.name,p.sku,c.unit INTO demo_name,demo_sku,demo_unit FROM products p JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id WHERE p.product_id=demo_products[demo_index];
            INSERT INTO purchase_items(purchase_id,product_id,warehouse_id,supplier_option_id,base_quantity,presentation_name_snapshot,base_factor_snapshot,unit_snapshot,product_name_snapshot,sku_snapshot,attributes_snapshot,quantity,unit_cost,received_quantity)
                VALUES (demo_purchase,demo_products[demo_index],demo_warehouses[1+demo_n%2],demo_options[demo_index],demo_quantity,'BASE',1,demo_unit,demo_name,demo_sku,'{}',demo_quantity,demo_price,0);
            demo_total := demo_total+round(demo_quantity*demo_price,2);
        END LOOP;
        UPDATE purchases SET total_amount=demo_total WHERE purchase_id=demo_purchase;
        IF demo_status='CONFIRMED' THEN
            INSERT INTO accounts_payable(supplier_id,purchase_id,financial_category_id,description,amount,due_date,status,issued_at,payment_method,legacy,created_by,updated_by)
                VALUES (demo_supplier,demo_purchase,demo_expense,'DEMO Dashboard - compra ficticia',demo_total,demo_due,CASE WHEN demo_paid THEN 'PAID' ELSE 'OPEN' END,demo_date+TIME '09:00','BANK_TRANSFER',FALSE,demo_user,demo_user)
                RETURNING account_payable_id INTO demo_title;
            IF demo_paid THEN
                INSERT INTO accounts_payable_payments(account_payable_id,bank_account_id,user_id,paid_amount,paid_at,payment_method,notes,legacy,recorded_at)
                    VALUES (demo_title,demo_bank,demo_user,demo_total,least(demo_today,demo_date+3),'BANK_TRANSFER','DEMO Dashboard - pagamento ficticio',FALSE,least(demo_today,demo_date+3)+TIME '12:00');
            END IF;
        END IF;
    END LOOP;

    -- Ajustes finais criam alertas mantendo quantity >= reserved_quantity.
    -- Afetam exclusivamente os 16 registros de estoque inseridos acima.
    FOR demo_n IN 1..8 LOOP
        FOR demo_depot IN 1..2 LOOP
            SELECT stock_item_id,quantity,reserved_quantity INTO demo_stock,demo_before,demo_reserved
                FROM stock_items WHERE product_id=demo_products[demo_n] AND warehouse_id=demo_warehouses[demo_depot];
            demo_after := demo_reserved + CASE WHEN demo_n%4=0 THEN 0 WHEN demo_n%4=1 THEN 12 ELSE 75+demo_n*5 END;
            UPDATE stock_items SET quantity=demo_after WHERE stock_item_id=demo_stock;
            INSERT INTO stock_movements(stock_item_id,type,reason,quantity,previous_balance,new_balance,user_id,occurred_at,notes)
                VALUES (demo_stock,'ADJUSTMENT','MANUAL_ADJUSTMENT',demo_after-demo_before,demo_before,demo_after,demo_user,demo_today+TIME '18:00','DEMO Dashboard - ajuste ficticio para alertas de estoque');
        END LOOP;
    END LOOP;

    -- Seis geracoes: historicos cobertos pelo estoque; apos os ajustes de hoje,
    -- a ultima gera dois rascunhos (um por deposito). Snapshots derivam do historico DEMO.
    FOR demo_n IN REVERSE 5..0 LOOP
        demo_date := demo_today-demo_n*7;
        INSERT INTO purchase_suggestions(generated_at,updated_at,reference_period,status,generated_by,period_from,period_to)
            VALUES (demo_date+TIME '19:00',demo_date+TIME '19:00',to_char(demo_date,'YYYY-MM'),'PENDING',demo_user,
                (demo_date-90)::timestamp,demo_date::timestamp)
            RETURNING purchase_suggestion_id INTO demo_suggestion;
        FOR demo_depot IN 1..2 LOOP
            demo_purchase := NULL;
            demo_total := 0;
            FOR demo_line IN 1..2 LOOP
                demo_index := 1+(demo_line-1)*4;
                demo_price := round((5.9+demo_index*3.1)*0.6,2);
                SELECT c.name,p.sku,c.unit INTO demo_name,demo_sku,demo_unit FROM products p JOIN product_catalogs c ON c.product_catalog_id=p.catalog_id WHERE p.product_id=demo_products[demo_index];
                SELECT stock_item_id INTO demo_stock FROM stock_items WHERE product_id=demo_products[demo_index] AND warehouse_id=demo_warehouses[demo_depot];
                SELECT coalesce(sum(CASE WHEN type='OUTBOUND' THEN -quantity ELSE quantity END),0)
                    INTO demo_before FROM stock_movements WHERE stock_item_id=demo_stock AND occurred_at<=demo_date+TIME '19:00';
                SELECT coalesce(sum(quantity),0) INTO demo_reserved FROM stock_reservations
                    WHERE stock_item_id=demo_stock AND created_at<=demo_date+TIME '19:00'
                    AND (status='ACTIVE' OR updated_at>demo_date+TIME '19:00');
                SELECT coalesce(sum(quantity),0) INTO demo_sold FROM stock_movements
                    WHERE stock_item_id=demo_stock AND reason='SALE' AND type='OUTBOUND'
                    AND occurred_at>=(demo_date-90)::timestamp AND occurred_at<demo_date::timestamp;
                demo_available := demo_before-demo_reserved;
                demo_target := greatest(30,round(demo_sold*14/90,10));
                demo_shortage := greatest(0,demo_target-demo_available);
                demo_quantity := ceil(demo_shortage);
                demo_item := NULL;
                IF demo_quantity>0 THEN
                    IF demo_purchase IS NULL THEN
                        INSERT INTO purchases(supplier_id,user_id,purchase_suggestion_id,status,created_at,expected_delivery_date,total_amount)
                            VALUES (demo_supplier,demo_user,demo_suggestion,'DRAFT',demo_date+TIME '19:00',demo_date+7,0) RETURNING purchase_id INTO demo_purchase;
                    END IF;
                    INSERT INTO purchase_items(purchase_id,product_id,warehouse_id,supplier_option_id,base_quantity,presentation_name_snapshot,base_factor_snapshot,unit_snapshot,product_name_snapshot,sku_snapshot,attributes_snapshot,quantity,unit_cost,received_quantity)
                        VALUES (demo_purchase,demo_products[demo_index],demo_warehouses[demo_depot],demo_options[demo_index],demo_quantity,'BASE',1,demo_unit,demo_name,demo_sku,'{}',demo_quantity,demo_price,0)
                        RETURNING purchase_item_id INTO demo_item;
                    demo_total := demo_total+round(demo_quantity*demo_price,2);
                END IF;
                INSERT INTO purchase_suggestion_items(purchase_suggestion_id,product_id,supplier_id,warehouse_id,
                    period_sold_quantity,avg_daily_consumption,current_stock,min_stock,suggested_quantity,
                    reserved_stock,available_stock,target_stock,covered_quantity,lead_days,selection_rule,
                    supplier_option_id,unit_cost,base_factor,commercial_quantity,purchase_id,purchase_item_id,result,shortage_quantity)
                    VALUES (demo_suggestion,demo_products[demo_index],demo_supplier,demo_warehouses[demo_depot],
                        demo_sold,round(demo_sold/90,10),demo_before,30,demo_quantity,demo_reserved,demo_available,demo_target,0,7,'PREFERRED',
                        demo_options[demo_index],demo_price,1,demo_quantity,CASE WHEN demo_quantity>0 THEN demo_purchase ELSE NULL END,demo_item,
                        CASE WHEN demo_quantity>0 THEN 'GENERATED' ELSE 'COVERED' END,demo_shortage);
            END LOOP;
            IF demo_purchase IS NOT NULL THEN
                UPDATE purchases SET total_amount=demo_total WHERE purchase_id=demo_purchase;
            END IF;
        END LOOP;
    END LOOP;
END;
$dashboard_demo$;
