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
    active        BOOLEAN NOT NULL DEFAULT TRUE
);


-- =====================================================================
-- MÓDULO: PRODUTOS & ESTOQUE
-- =====================================================================

CREATE TABLE categories (
    category_id   BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL
);

CREATE TABLE brands (
    brand_id      BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL
);

CREATE TABLE products (
    product_id       BIGSERIAL PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    sku              VARCHAR(50) NOT NULL UNIQUE,
    category_id      BIGINT NOT NULL REFERENCES categories(category_id),
    brand_id         BIGINT NOT NULL REFERENCES brands(brand_id),
    cost             NUMERIC(12,2) NOT NULL DEFAULT 0,
    price            NUMERIC(12,2) NOT NULL DEFAULT 0,
    weight_kg        NUMERIC(10,3),
    color            VARCHAR(50),
    dimension_x      NUMERIC(10,2),
    dimension_y      NUMERIC(10,2),
    dimension_z      NUMERIC(10,2),

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

CREATE TABLE warehouses (
    warehouse_id  BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(255)
);

CREATE TABLE warehouse_addresses (
    warehouse_address_id BIGSERIAL PRIMARY KEY,
    warehouse_id  BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    state         VARCHAR(50),
    city          VARCHAR(100),
    street        VARCHAR(150),
    neighborhood  VARCHAR(100),
    number        VARCHAR(10),
    complement    VARCHAR(100),
    zip_code      VARCHAR(9)
);

CREATE TABLE stock_items (
    stock_item_id     BIGSERIAL PRIMARY KEY,
    warehouse_id      BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    product_id        BIGINT NOT NULL REFERENCES products(product_id),
    quantity          INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    min_quantity      INT NOT NULL DEFAULT 0,
    max_quantity      INT,
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

CREATE TABLE product_suppliers (
    product_supplier_id   BIGSERIAL PRIMARY KEY,
    product_id            BIGINT NOT NULL REFERENCES products(product_id),
    supplier_id           BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    cost                  NUMERIC(12,2) NOT NULL,
    avg_delivery_days     INT,
    supplier_product_code VARCHAR(50),
    preferred             BOOLEAN NOT NULL DEFAULT FALSE,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_product_supplier UNIQUE (product_id, supplier_id)
);

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

CREATE TABLE price_table_items (
    price_table_item_id BIGSERIAL PRIMARY KEY,
    price_table_id BIGINT NOT NULL REFERENCES price_tables(price_table_id),
    product_id    BIGINT NOT NULL REFERENCES products(product_id),
    price         NUMERIC(12,2) NOT NULL,
    CONSTRAINT uk_price_table_item UNIQUE (price_table_id, product_id)
);

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
    quantity      INT NOT NULL,
    unit_price    NUMERIC(12,2) NOT NULL,
    discount      NUMERIC(12,2) NOT NULL DEFAULT 0
);


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
    quantity          INT NOT NULL,
    unit_cost         NUMERIC(12,2) NOT NULL,
    received_quantity INT NOT NULL DEFAULT 0
);

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
    stock_item_id      BIGINT NOT NULL REFERENCES stock_items(stock_item_id),
    type               VARCHAR(20) NOT NULL CHECK (type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT')),
    reason             VARCHAR(30) CHECK (reason IN ('PURCHASE', 'SALE', 'RETURN', 'MANUAL_ADJUSTMENT', 'LOSS')),
    quantity           INT NOT NULL,
    previous_balance   INT NOT NULL,
    new_balance        INT NOT NULL,
    order_id           BIGINT REFERENCES orders(order_id),
    purchase_id        BIGINT REFERENCES purchases(purchase_id),
    user_id            BIGINT NOT NULL REFERENCES users(user_id),
    occurred_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    notes              VARCHAR(255),
    CONSTRAINT chk_stock_movement_origin CHECK (num_nonnulls(order_id, purchase_id) <= 1)
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
