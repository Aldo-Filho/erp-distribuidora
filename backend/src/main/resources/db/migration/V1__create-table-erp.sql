-- Estrutura consolidada para banco NOVO. Substitui a antiga sequencia V1-V8.
-- Nao aplicar sobre um banco com o historico antigo do Flyway.

-- =====================================================================
-- MÓDULO: CONFIGURAÇÕES (roles, permissions, users)
-- =====================================================================

CREATE TABLE roles (
    role_id       BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    module        VARCHAR(50) NOT NULL,
    action        VARCHAR(50) NOT NULL,
    permission_key VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(role_id),
    permission_id BIGINT NOT NULL REFERENCES permissions(permission_id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    user_id       BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    last_login_at TIMESTAMP,
    role_id       BIGINT NOT NULL REFERENCES roles(role_id),
    credential_version BIGINT NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_users_email_normalized ON users (lower(btrim(email)));


-- =====================================================================
-- MÓDULO: PRODUTOS & ESTOQUE
-- =====================================================================

CREATE TABLE categories (
    category_id   BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_category_name_normalized
    ON categories (lower(btrim(name)));

CREATE TABLE brands (
    brand_id      BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_brand_name_normalized
    ON brands (lower(btrim(name)));

CREATE SEQUENCE product_sku_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO CYCLE;

CREATE TABLE product_catalogs (
    product_catalog_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    brand_id BIGINT NOT NULL REFERENCES brands(brand_id),
    category_id BIGINT REFERENCES categories(category_id),
    unit VARCHAR(10) CHECK (unit IN ('UN', 'KG', 'G', 'L', 'ML', 'M')),
    type VARCHAR(10) NOT NULL CHECK (type IN ('SIMPLE', 'VARIANT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE product_catalog_characteristics (
    product_catalog_id BIGINT NOT NULL REFERENCES product_catalogs(product_catalog_id),
    characteristic VARCHAR(20) NOT NULL CHECK (characteristic IN ('COLOR','SIZE','VOLTAGE','POWER','CAPACITY','MODEL')),
    PRIMARY KEY (product_catalog_id, characteristic)
);

CREATE TABLE products (
    product_id       BIGSERIAL PRIMARY KEY,
    catalog_id       BIGINT NOT NULL REFERENCES product_catalogs(product_catalog_id),
    variant_key      TEXT NOT NULL DEFAULT '',
    sku              VARCHAR(50) NOT NULL UNIQUE,
    cost             NUMERIC(12,2) NOT NULL DEFAULT 0,
    price            NUMERIC(12,2) NOT NULL DEFAULT 0,
    weight_kg        NUMERIC(10,3),
    color            VARCHAR(50),
    dimension_x      NUMERIC(10,2),
    dimension_y      NUMERIC(10,2),
    dimension_z      NUMERIC(10,2),
    size             VARCHAR(10),

    -- dados fiscais
    ncm              VARCHAR(8),
    cest             VARCHAR(7),
    default_cfop     VARCHAR(4),
    cst_csosn        VARCHAR(4),
    origin           VARCHAR(30) CHECK (origin IN ('NATIONAL', 'FOREIGN_DIRECT_IMPORT', 'FOREIGN_DOMESTIC_MARKET')),
    commercial_unit  VARCHAR(10),
    icms_rate        NUMERIC(5,2),
    ipi_rate         NUMERIC(5,2),
    pis_rate         NUMERIC(5,2),
    cofins_rate      NUMERIC(5,2),

    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP
);

CREATE UNIQUE INDEX uk_product_catalog_variant ON products(catalog_id, variant_key);

CREATE TABLE product_variant_values (
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    characteristic VARCHAR(20) NOT NULL CHECK (characteristic IN ('COLOR','SIZE','VOLTAGE','POWER','CAPACITY','MODEL')),
    value TEXT NOT NULL CHECK (btrim(value) <> ''),
    PRIMARY KEY(product_id,characteristic)
);

CREATE TABLE product_presentations (
    product_presentation_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    name VARCHAR(150) NOT NULL,
    base_factor NUMERIC(14,1) NOT NULL CHECK (base_factor > 0),
    base_presentation BOOLEAN NOT NULL DEFAULT FALSE,
    price NUMERIC(12,2) CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (NOT base_presentation OR (base_factor = 1 AND price IS NULL))
);

CREATE UNIQUE INDEX uk_product_base_presentation ON product_presentations(product_id) WHERE base_presentation;

CREATE UNIQUE INDEX uk_product_presentation_identity ON product_presentations(product_id, lower(btrim(name)), base_factor);

CREATE TABLE warehouses (
    warehouse_id  BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(255)
);

CREATE UNIQUE INDEX uk_warehouse_name_normalized
    ON warehouses (lower(btrim(name)));

CREATE TABLE warehouse_addresses (
    warehouse_address_id BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT NOT NULL UNIQUE REFERENCES warehouses(warehouse_id),
    state         VARCHAR(50)  NOT NULL,
    city          VARCHAR(100) NOT NULL,
    street        VARCHAR(150) NOT NULL,
    neighborhood  VARCHAR(100) NOT NULL,
    number        VARCHAR(10)  NOT NULL,
    complement    VARCHAR(100) NOT NULL,
    zip_code      VARCHAR(9)   NOT NULL
);

CREATE UNIQUE INDEX uk_warehouse_address_identity
    ON warehouse_addresses (
        lower(btrim(state)),
        lower(btrim(city)),
        lower(btrim(street)),
        lower(btrim(zip_code)),
        lower(btrim(number)),
        lower(btrim(complement))
    );

CREATE TABLE stock_items (
    stock_item_id     BIGSERIAL PRIMARY KEY,
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    quantity          NUMERIC(14,1) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(14,1) NOT NULL DEFAULT 0,
    min_quantity      NUMERIC(14,1) NOT NULL DEFAULT 0,
    max_quantity      NUMERIC(14,1),
    CONSTRAINT uk_stock_item UNIQUE (warehouse_id, product_id)
);


-- =====================================================================
-- MÓDULO: FORNECEDORES
-- =====================================================================

CREATE TABLE suppliers (
    supplier_id       BIGSERIAL PRIMARY KEY,
    legal_name        VARCHAR(150) NOT NULL,
    trade_name        VARCHAR(150),
    tax_id            VARCHAR(14) NOT NULL UNIQUE, -- CNPJ
    email             VARCHAR(150),
    phone             VARCHAR(20),
    whatsapp          VARCHAR(20),
    avg_delivery_days INT,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE TABLE supplier_addresses (
    supplier_address_id BIGSERIAL PRIMARY KEY,
    supplier_id   BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    country       VARCHAR(50),
    state         VARCHAR(50),
    city          VARCHAR(100),
    street        VARCHAR(150),
    neighborhood  VARCHAR(100),
    number        VARCHAR(10),
    complement    VARCHAR(100),
    zip_code      VARCHAR(9),
    address_type  VARCHAR(20) CHECK (address_type IN ('BILLING', 'SHIPPING', 'MAIN'))
);

CREATE UNIQUE INDEX uk_supplier_address_identity
    ON supplier_addresses (
        supplier_id,
        lower(btrim(coalesce(address_type, ''))),
        lower(btrim(coalesce(country, ''))),
        lower(btrim(coalesce(state, ''))),
        lower(btrim(coalesce(city, ''))),
        lower(btrim(coalesce(street, ''))),
        lower(btrim(coalesce(zip_code, ''))),
        lower(btrim(coalesce(number, ''))),
        lower(btrim(coalesce(complement, '')))
    );

CREATE TABLE product_suppliers (
    product_supplier_id   BIGSERIAL PRIMARY KEY,
    product_id            BIGINT NOT NULL REFERENCES products(product_id),
    supplier_id           BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    legacy_option_id      BIGINT,
    preferred             BOOLEAN NOT NULL DEFAULT FALSE,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_product_supplier UNIQUE (product_id, supplier_id)
);

CREATE TABLE product_supplier_options (
    product_supplier_option_id BIGSERIAL PRIMARY KEY,
    product_supplier_id BIGINT NOT NULL REFERENCES product_suppliers(product_supplier_id),
    name VARCHAR(150) NOT NULL,
    base_factor NUMERIC(14,1) CHECK (base_factor > 0),
    base_presentation BOOLEAN,
    cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),
    avg_delivery_days INT CHECK (avg_delivery_days >= 0),
    supplier_product_code VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    conversion_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (NOT conversion_confirmed OR (base_factor IS NOT NULL AND base_presentation IS NOT NULL)),
    CHECK (base_presentation IS DISTINCT FROM TRUE OR base_factor = 1)
);

CREATE UNIQUE INDEX uk_supplier_option_identity ON product_supplier_options(product_supplier_id, lower(btrim(name)), base_factor);

-- Circular legacy association is declared after both tables exist. New links leave it null.
ALTER TABLE product_suppliers ADD CONSTRAINT fk_product_supplier_legacy_option
    FOREIGN KEY (legacy_option_id) REFERENCES product_supplier_options(product_supplier_option_id);

CREATE UNIQUE INDEX uk_product_preferred_supplier ON product_suppliers(product_id) WHERE preferred;

CREATE TABLE supplier_categories (
    supplier_id   BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    category_id   BIGINT NOT NULL REFERENCES categories(category_id),
    PRIMARY KEY (supplier_id, category_id)
);

CREATE TABLE supplier_brands (
    supplier_id   BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    brand_id      BIGINT NOT NULL REFERENCES brands(brand_id),
    PRIMARY KEY (supplier_id, brand_id)
);


-- =====================================================================
-- MÓDULO: CLIENTES & TABELA DE PREÇOS
-- =====================================================================

CREATE TABLE price_tables (
    price_table_id BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    start_date    DATE,
    end_date      DATE,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

CREATE UNIQUE INDEX uk_price_table_name_normalized
    ON price_tables (lower(btrim(name)));

CREATE TABLE price_table_items (
    price_table_item_id BIGSERIAL PRIMARY KEY,
    price_table_id BIGINT NOT NULL REFERENCES price_tables(price_table_id),
    product_id    BIGINT NOT NULL REFERENCES products(product_id),
    presentation_id BIGINT REFERENCES product_presentations(product_presentation_id),
    price         NUMERIC(12,2) NOT NULL
);

CREATE UNIQUE INDEX uk_price_table_base ON price_table_items(price_table_id, product_id) WHERE presentation_id IS NULL;
CREATE UNIQUE INDEX uk_price_table_presentation ON price_table_items(price_table_id, presentation_id) WHERE presentation_id IS NOT NULL;

CREATE TABLE customers (
    customer_id       BIGSERIAL PRIMARY KEY,
    person_type       VARCHAR(2) NOT NULL CHECK (person_type IN ('PF', 'PJ')),
    tax_id             VARCHAR(14) NOT NULL, -- CPF ou CNPJ
    legal_name         VARCHAR(150) NOT NULL,
    trade_name         VARCHAR(150),
    birth_date         DATE,
    municipal_reg      VARCHAR(20),
    state_reg          VARCHAR(20),
    email              VARCHAR(150),
    phone              VARCHAR(20),
    whatsapp           VARCHAR(20),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP,
    price_table_id     BIGINT REFERENCES price_tables(price_table_id),
    CONSTRAINT uk_customer_tax_id UNIQUE (tax_id)
);

CREATE TABLE customer_addresses (
    customer_address_id BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT NOT NULL REFERENCES customers(customer_id),
    state         VARCHAR(50)  NOT NULL,
    city          VARCHAR(100) NOT NULL,
    street        VARCHAR(150) NOT NULL,
    neighborhood  VARCHAR(100) NOT NULL,
    number        VARCHAR(10)  NOT NULL,
    complement    VARCHAR(100),
    zip_code      VARCHAR(9)   NOT NULL,
    address_type  VARCHAR(20) NOT NULL
        CHECK (address_type IN ('BILLING', 'SHIPPING', 'MAIN')),
    CONSTRAINT uk_customer_address_type UNIQUE (customer_id, address_type)
);


-- =====================================================================
-- MÓDULO: PEDIDOS
-- =====================================================================

CREATE TABLE orders (
    order_id      BIGSERIAL PRIMARY KEY,
    total_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(20) CHECK (payment_method IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO', 'PIX', 'BANK_TRANSFER')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'INVOICED', 'CANCELED')),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    customer_id   BIGINT NOT NULL REFERENCES customers(customer_id),
    user_id       BIGINT REFERENCES users(user_id) -- opcional: pedido pode nascer de um atendimento automático (WhatsApp)
);

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(order_id),
    product_id    BIGINT NOT NULL REFERENCES products(product_id),
    warehouse_id  BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    presentation_id BIGINT REFERENCES product_presentations(product_presentation_id),
    base_quantity NUMERIC(14,1) NOT NULL,
    presentation_name_snapshot VARCHAR(150),
    base_factor_snapshot NUMERIC(14,1) NOT NULL,
    unit_snapshot VARCHAR(10),
    product_name_snapshot VARCHAR(150),
    sku_snapshot VARCHAR(50),
    attributes_snapshot TEXT,
    quantity      NUMERIC(14,1) NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    discount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'AMOUNT',
    CONSTRAINT chk_order_item_discount_type
        CHECK (discount_type IN ('AMOUNT', 'PERCENTAGE'))
);

CREATE UNIQUE INDEX uk_order_item_presentation ON order_items(order_id, product_id, warehouse_id, coalesce(presentation_id, 0));

CREATE TABLE stock_reservations (
    stock_reservation_id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL REFERENCES order_items(order_item_id),
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(stock_item_id),
    quantity NUMERIC(14,1) NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_stock_reservation_order_item UNIQUE (order_item_id)
);

CREATE INDEX idx_stock_reservation_stock_status ON stock_reservations (stock_item_id, status);


-- =====================================================================
-- MÓDULO: COMPRAS
-- =====================================================================

CREATE TABLE purchase_suggestions (
    purchase_suggestion_id BIGSERIAL PRIMARY KEY,
    generated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    reference_period  VARCHAR(20), -- ex: '2026-06'
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DISCARDED')),
    reviewer_user_id  BIGINT REFERENCES users(user_id)
);

CREATE TABLE purchases (
    purchase_id       BIGSERIAL PRIMARY KEY,
    supplier_id       BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    purchase_suggestion_id BIGINT REFERENCES purchase_suggestions(purchase_suggestion_id),
    status            VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'CONFIRMED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELED')),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    expected_delivery_date DATE,
    total_amount      NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE purchase_items (
    purchase_item_id  BIGSERIAL PRIMARY KEY,
    purchase_id       BIGINT NOT NULL REFERENCES purchases(purchase_id),
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    supplier_option_id BIGINT REFERENCES product_supplier_options(product_supplier_option_id),
    base_quantity NUMERIC(14,1) NOT NULL,
    presentation_name_snapshot VARCHAR(150),
    base_factor_snapshot NUMERIC(14,1) NOT NULL,
    unit_snapshot VARCHAR(10),
    product_name_snapshot VARCHAR(150),
    sku_snapshot VARCHAR(50),
    attributes_snapshot TEXT,
    quantity          NUMERIC(14,1) NOT NULL,
    unit_cost         NUMERIC(12,2) NOT NULL,
    received_quantity NUMERIC(14,1) NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_purchase_item_option ON purchase_items(purchase_id, product_id, warehouse_id, coalesce(supplier_option_id, 0));

CREATE TABLE purchase_receipts (
    purchase_receipt_id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES purchases(purchase_id),
    request_id UUID NOT NULL,
    operator_user_id BIGINT NOT NULL REFERENCES users(user_id),
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes VARCHAR(255),
    CONSTRAINT uk_purchase_receipt_request UNIQUE (purchase_id, request_id)
);

CREATE TABLE purchase_receipt_items (
    purchase_receipt_item_id BIGSERIAL PRIMARY KEY,
    purchase_receipt_id BIGINT NOT NULL REFERENCES purchase_receipts(purchase_receipt_id),
    purchase_item_id BIGINT NOT NULL REFERENCES purchase_items(purchase_item_id),
    stock_item_id BIGINT NOT NULL REFERENCES stock_items(stock_item_id),
    quantity NUMERIC(14,1) NOT NULL CHECK (quantity > 0),
    CONSTRAINT uk_purchase_receipt_item UNIQUE (purchase_receipt_id, purchase_item_id)
);

CREATE INDEX idx_purchase_receipt_item_stock ON purchase_receipt_items (stock_item_id);

CREATE TABLE purchase_suggestion_items (
    purchase_suggestion_item_id BIGSERIAL PRIMARY KEY,
    purchase_suggestion_id BIGINT NOT NULL REFERENCES purchase_suggestions(purchase_suggestion_id),
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    supplier_id       BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    period_sold_quantity  INT NOT NULL DEFAULT 0,
    avg_daily_consumption NUMERIC(10,2),
    current_stock     INT,
    min_stock         INT,
    suggested_quantity INT NOT NULL
);


-- =====================================================================
-- MOVIMENTAÇÕES DE ESTOQUE (depende de orders/purchases já criadas)
-- =====================================================================

CREATE TABLE stock_movements (
    stock_movement_id BIGSERIAL PRIMARY KEY,
    request_id         UUID,
    stock_item_id      BIGINT NOT NULL REFERENCES stock_items(stock_item_id),
    type               VARCHAR(20) NOT NULL CHECK (type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT')),
    reason             VARCHAR(30) CHECK (reason IN ('PURCHASE', 'SALE', 'RETURN', 'MANUAL_ADJUSTMENT', 'LOSS')),
    quantity           NUMERIC(14,1) NOT NULL,
    previous_balance   NUMERIC(14,1) NOT NULL,
    new_balance        NUMERIC(14,1) NOT NULL,
    order_id           BIGINT REFERENCES orders(order_id),
    purchase_id        BIGINT REFERENCES purchases(purchase_id),
    user_id            BIGINT NOT NULL REFERENCES users(user_id),
    occurred_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    notes              VARCHAR(255),
    stock_reservation_id BIGINT REFERENCES stock_reservations(stock_reservation_id),
    purchase_receipt_item_id BIGINT REFERENCES purchase_receipt_items(purchase_receipt_item_id),
    CONSTRAINT uk_stock_movement_request_id UNIQUE (request_id),
    CONSTRAINT uk_stock_movement_receipt_item UNIQUE (purchase_receipt_item_id),
    CONSTRAINT uk_stock_movement_reservation UNIQUE (stock_reservation_id),
    CONSTRAINT chk_stock_movement_origin CHECK (num_nonnulls(order_id, purchase_id) <= 1),
    CONSTRAINT chk_stock_movement_receipt CHECK (
        purchase_receipt_item_id IS NULL OR (
            type = 'INBOUND' AND reason IS NOT NULL AND reason = 'PURCHASE'
            AND purchase_id IS NOT NULL AND order_id IS NULL AND stock_reservation_id IS NULL
        )
    ),
    CONSTRAINT chk_stock_movement_reservation CHECK (
        stock_reservation_id IS NULL OR (
            type = 'OUTBOUND' AND reason IS NOT NULL AND reason = 'SALE'
            AND order_id IS NOT NULL AND purchase_id IS NULL
        )
    )
);


-- =====================================================================
-- MÓDULO: FINANCEIRO
-- =====================================================================

CREATE TABLE financial_categories (
    financial_category_id BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(10) NOT NULL CHECK (type IN ('REVENUE', 'EXPENSE')),
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE bank_accounts (
    bank_account_id BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(20) NOT NULL CHECK (type IN ('CASH', 'CHECKING_ACCOUNT', 'SAVINGS_ACCOUNT')),
    initial_balance NUMERIC(12,2) NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE accounts_receivable (
    account_receivable_id BIGSERIAL PRIMARY KEY,
    customer_id       BIGINT NOT NULL REFERENCES customers(customer_id),
    order_id          BIGINT REFERENCES orders(order_id),
    financial_category_id BIGINT NOT NULL REFERENCES financial_categories(financial_category_id),
    description       VARCHAR(255),
    amount            NUMERIC(12,2) NOT NULL,
    due_date          DATE NOT NULL,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELED')),
    issued_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE TABLE accounts_payable (
    account_payable_id BIGSERIAL PRIMARY KEY,
    supplier_id       BIGINT REFERENCES suppliers(supplier_id),
    purchase_id       BIGINT REFERENCES purchases(purchase_id),
    financial_category_id BIGINT NOT NULL REFERENCES financial_categories(financial_category_id),
    description       VARCHAR(255),
    amount            NUMERIC(12,2) NOT NULL,
    due_date          DATE NOT NULL,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELED')),
    issued_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE TABLE accounts_receivable_payments (
    payment_id        BIGSERIAL PRIMARY KEY,
    account_receivable_id BIGINT NOT NULL REFERENCES accounts_receivable(account_receivable_id),
    bank_account_id   BIGINT NOT NULL REFERENCES bank_accounts(bank_account_id),
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    paid_amount       NUMERIC(12,2) NOT NULL,
    paid_at           DATE NOT NULL,
    payment_method    VARCHAR(20) CHECK (payment_method IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO', 'PIX', 'BANK_TRANSFER')),
    notes             VARCHAR(255)
);

CREATE TABLE accounts_payable_payments (
    payment_id        BIGSERIAL PRIMARY KEY,
    account_payable_id BIGINT NOT NULL REFERENCES accounts_payable(account_payable_id),
    bank_account_id   BIGINT NOT NULL REFERENCES bank_accounts(bank_account_id),
    user_id           BIGINT NOT NULL REFERENCES users(user_id),
    paid_amount       NUMERIC(12,2) NOT NULL,
    paid_at           DATE NOT NULL,
    payment_method    VARCHAR(20) CHECK (payment_method IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BOLETO', 'PIX', 'BANK_TRANSFER')),
    notes             VARCHAR(255)
);


-- =====================================================================
-- MÓDULO: FISCAL / NFe
-- =====================================================================

CREATE TABLE companies (
    company_id        BIGSERIAL PRIMARY KEY,
    legal_name         VARCHAR(150) NOT NULL,
    trade_name         VARCHAR(150),
    tax_id             VARCHAR(14) NOT NULL UNIQUE, -- CNPJ
    state_reg          VARCHAR(20),
    municipal_reg      VARCHAR(20),
    tax_regime         VARCHAR(30) NOT NULL CHECK (tax_regime IN ('SIMPLES_NACIONAL', 'LUCRO_PRESUMIDO', 'LUCRO_REAL')),
    nfe_environment    VARCHAR(20) NOT NULL DEFAULT 'HOMOLOGATION' CHECK (nfe_environment IN ('HOMOLOGATION', 'PRODUCTION')),
    nfe_default_series VARCHAR(5),
    nfe_next_number    BIGINT NOT NULL DEFAULT 1,
    digital_certificate_ref VARCHAR(255) -- referência/caminho, nunca a chave privada em texto puro
);

CREATE TABLE company_addresses (
    company_address_id BIGSERIAL PRIMARY KEY,
    company_id    BIGINT NOT NULL REFERENCES companies(company_id),
    country       VARCHAR(50),
    state         VARCHAR(50),
    city          VARCHAR(100),
    street        VARCHAR(150),
    neighborhood  VARCHAR(100),
    number        VARCHAR(10),
    complement    VARCHAR(100),
    zip_code      VARCHAR(9)
);

CREATE TABLE invoices (
    invoice_id            BIGSERIAL PRIMARY KEY,
    order_id              BIGINT NOT NULL REFERENCES orders(order_id),
    company_id            BIGINT NOT NULL REFERENCES companies(company_id),
    customer_id           BIGINT NOT NULL REFERENCES customers(customer_id),
    number                BIGINT NOT NULL,
    series                VARCHAR(5) NOT NULL,
    access_key            VARCHAR(44) UNIQUE,
    operation_nature      VARCHAR(100),
    purpose               VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (purpose IN ('NORMAL', 'COMPLEMENTARY', 'ADJUSTMENT', 'RETURN')),
    environment           VARCHAR(20) NOT NULL CHECK (environment IN ('HOMOLOGATION', 'PRODUCTION')),
    status                VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'AUTHORIZED', 'REJECTED', 'CANCELED', 'DENIED', 'VOIDED')),
    authorization_protocol VARCHAR(50),
    issued_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    authorized_at         TIMESTAMP,
    products_total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount          NUMERIC(12,2) NOT NULL DEFAULT 0,
    freight_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount       NUMERIC(12,2) NOT NULL DEFAULT 0,
    cancellation_reason   VARCHAR(255),
    authorized_xml_path   VARCHAR(255),
    CONSTRAINT uk_invoice_number_series UNIQUE (company_id, series, number)
);

CREATE TABLE invoice_items (
    invoice_item_id   BIGSERIAL PRIMARY KEY,
    invoice_id        BIGINT NOT NULL REFERENCES invoices(invoice_id),
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    quantity          INT NOT NULL,
    unit_price        NUMERIC(12,2) NOT NULL,
    total_price       NUMERIC(12,2) NOT NULL,

    -- snapshot fiscal no momento da emissão (não referencia products em tempo real)
    ncm_snapshot          VARCHAR(8),
    cfop_snapshot         VARCHAR(4),
    cst_csosn_snapshot    VARCHAR(4),
    icms_rate_snapshot    NUMERIC(5,2),
    icms_amount           NUMERIC(12,2),
    ipi_rate_snapshot     NUMERIC(5,2),
    ipi_amount            NUMERIC(12,2),
    pis_rate_snapshot     NUMERIC(5,2),
    pis_amount            NUMERIC(12,2),
    cofins_rate_snapshot  NUMERIC(5,2),
    cofins_amount         NUMERIC(12,2)
);

CREATE TABLE invoice_events (
    invoice_event_id  BIGSERIAL PRIMARY KEY,
    invoice_id        BIGINT NOT NULL REFERENCES invoices(invoice_id),
    event_type        VARCHAR(30) NOT NULL CHECK (event_type IN ('CANCELLATION', 'CORRECTION_LETTER', 'VOIDING')),
    occurred_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    protocol          VARCHAR(50),
    description       VARCHAR(255)
);

-- AUTENTICACAO, SESSOES E AUDITORIA

CREATE TABLE spring_session (
    primary_id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100)
);
CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);
CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL REFERENCES spring_session(primary_id) ON DELETE CASCADE,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);
CREATE TABLE security_settings (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    idle_minutes INT NOT NULL CHECK (idle_minutes BETWEEN 15 AND 240),
    absolute_minutes INT NOT NULL CHECK (absolute_minutes BETWEEN 60 AND 1440),
    max_sessions INT NOT NULL CHECK (max_sessions BETWEEN 1 AND 5),
    policy_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by BIGINT,
    CHECK (idle_minutes <= absolute_minutes)
);
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    session_id CHAR(36) NOT NULL UNIQUE REFERENCES spring_session(session_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    authenticated_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    credential_version BIGINT NOT NULL,
    browser VARCHAR(120) NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX ix_user_sessions_user ON user_sessions(user_id, last_activity_at);
CREATE INDEX ix_user_sessions_validity ON user_sessions(authenticated_at, last_activity_at) WHERE revoked_at IS NULL;
CREATE TABLE user_invitations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(role_id),
    created_by BIGINT,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
CREATE UNIQUE INDEX uk_pending_invitation_email ON user_invitations(lower(btrim(email)))
    WHERE consumed_at IS NULL AND canceled_at IS NULL;
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    credential_version BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE TABLE login_challenges (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    credential_version BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    context_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX ix_reset_user ON password_reset_tokens(user_id);
CREATE INDEX ix_challenge_user ON login_challenges(user_id);
CREATE TABLE security_audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(100) NOT NULL,
    target VARCHAR(150),
    outcome VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    correlation_id UUID NOT NULL,
    actor_name VARCHAR(150),
    actor_type VARCHAR(20),
    target_name VARCHAR(150),
    target_type VARCHAR(20),
    changes VARCHAR(500)
);
CREATE INDEX ix_security_audit_time ON security_audit_events(occurred_at DESC, id DESC);
