-- Preserve historical partial payments; all new documents use full manual settlement.
ALTER TABLE orders ADD COLUMN payment_due_date DATE;
ALTER TABLE financial_categories ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE financial_categories ADD COLUMN updated_by BIGINT REFERENCES users(user_id);
ALTER TABLE bank_accounts ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE bank_accounts ADD COLUMN updated_by BIGINT REFERENCES users(user_id);
ALTER TABLE financial_categories ADD COLUMN default_for_documents BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_financial_default_category ON financial_categories(type) WHERE default_for_documents;
ALTER TABLE financial_categories ADD CONSTRAINT ck_financial_default_active CHECK (NOT default_for_documents OR active);
-- Explicit initial configuration; no inference from arbitrary category IDs or names.
INSERT INTO financial_categories(name,type,default_for_documents) VALUES
('Receita de vendas','REVENUE',TRUE),('Compras de mercadorias','EXPENSE',TRUE);

ALTER TABLE accounts_receivable ADD CONSTRAINT uq_receivable_order UNIQUE (order_id);
ALTER TABLE accounts_receivable ADD COLUMN payment_method VARCHAR(20)
    CHECK (payment_method IN ('CASH','CREDIT_CARD','DEBIT_CARD','BOLETO','PIX','BANK_TRANSFER'));
ALTER TABLE accounts_receivable ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE accounts_receivable ALTER COLUMN legacy SET DEFAULT FALSE;
ALTER TABLE accounts_receivable ADD COLUMN created_by BIGINT REFERENCES users(user_id);
ALTER TABLE accounts_receivable ADD COLUMN updated_by BIGINT REFERENCES users(user_id);
ALTER TABLE accounts_receivable ADD CONSTRAINT ck_receivable_new_contract CHECK
    (legacy OR (status IN ('OPEN','PAID','CANCELED') AND amount >= 0 AND order_id IS NOT NULL
        AND created_by IS NOT NULL));
ALTER TABLE accounts_receivable_payments ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE accounts_receivable_payments ALTER COLUMN legacy SET DEFAULT FALSE;
-- Historical event time is unknown: do not fabricate it from migration execution time.
ALTER TABLE accounts_receivable_payments ADD COLUMN recorded_at TIMESTAMP;
CREATE UNIQUE INDEX uq_receivable_full_settlement ON accounts_receivable_payments(account_receivable_id) WHERE NOT legacy;
ALTER TABLE accounts_receivable_payments ADD CONSTRAINT ck_receivable_new_payment CHECK
    (legacy OR (paid_amount >= 0 AND payment_method IS NOT NULL AND recorded_at IS NOT NULL));
CREATE TABLE accounts_receivable_reversals (
    reversal_id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE REFERENCES accounts_receivable_payments(payment_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    reversed_at DATE NOT NULL,
    reason VARCHAR(255) NOT NULL CHECK (length(trim(reason)) > 0),
    recorded_at TIMESTAMP NOT NULL
);
CREATE INDEX ix_receivable_due ON accounts_receivable(due_date, status);
CREATE INDEX ix_receivable_payments_account ON accounts_receivable_payments(bank_account_id);

ALTER TABLE accounts_payable ADD CONSTRAINT uq_payable_purchase UNIQUE (purchase_id);
ALTER TABLE accounts_payable ADD COLUMN payment_method VARCHAR(20)
    CHECK (payment_method IN ('CASH','CREDIT_CARD','DEBIT_CARD','BOLETO','PIX','BANK_TRANSFER'));
ALTER TABLE accounts_payable ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE accounts_payable ALTER COLUMN legacy SET DEFAULT FALSE;
ALTER TABLE accounts_payable ADD COLUMN created_by BIGINT REFERENCES users(user_id);
ALTER TABLE accounts_payable ADD COLUMN updated_by BIGINT REFERENCES users(user_id);
ALTER TABLE accounts_payable ADD CONSTRAINT ck_payable_new_contract CHECK
    (legacy OR (status IN ('OPEN','PAID','CANCELED') AND amount >= 0 AND purchase_id IS NOT NULL
        AND created_by IS NOT NULL));
ALTER TABLE accounts_payable_payments ADD COLUMN legacy BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE accounts_payable_payments ALTER COLUMN legacy SET DEFAULT FALSE;
-- Historical event time is unknown: do not fabricate it from migration execution time.
ALTER TABLE accounts_payable_payments ADD COLUMN recorded_at TIMESTAMP;
CREATE UNIQUE INDEX uq_payable_full_settlement ON accounts_payable_payments(account_payable_id) WHERE NOT legacy;
ALTER TABLE accounts_payable_payments ADD CONSTRAINT ck_payable_new_payment CHECK
    (legacy OR (paid_amount >= 0 AND payment_method IS NOT NULL AND recorded_at IS NOT NULL));
CREATE TABLE accounts_payable_reversals (
    reversal_id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE REFERENCES accounts_payable_payments(payment_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    reversed_at DATE NOT NULL,
    reason VARCHAR(255) NOT NULL CHECK (length(trim(reason)) > 0),
    recorded_at TIMESTAMP NOT NULL
);
CREATE INDEX ix_payable_due ON accounts_payable(due_date, status);
CREATE INDEX ix_payable_payments_account ON accounts_payable_payments(bank_account_id);

UPDATE accounts_receivable r SET payment_method=o.payment_method FROM orders o WHERE r.order_id=o.order_id;
ALTER TABLE accounts_receivable ADD CONSTRAINT ck_receivable_new_method CHECK (legacy OR payment_method IS NOT NULL);
INSERT INTO permissions(module,action,permission_key) VALUES
('finance','read','finance.read'),('finance','configure','finance.configure'),
('finance','settle','finance.settle'),('finance','reverse','finance.reverse')
ON CONFLICT (permission_key) DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.role_id,p.permission_id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key IN ('finance.read','finance.configure','finance.settle','finance.reverse')
AND (SELECT count(DISTINCT e.permission_key) FROM role_permissions rp
    JOIN permissions e USING(permission_id) WHERE rp.role_id=r.role_id
    AND e.permission_key IN ('access.users.manage','access.roles.manage','security.settings.manage'))=3
ON CONFLICT DO NOTHING;

-- Read-only ledger: settled movements and remaining open commitments are separate kinds.
CREATE VIEW financial_cash_flow AS
SELECT 'receivable:payment:' || p.payment_id AS id, 'ACTUAL'::varchar AS kind,
    'RECEIVABLE'::varchar AS direction, a.account_receivable_id AS title_id,
    a.order_id AS document_id, a.customer_id AS party_id, a.financial_category_id AS category_id,
    p.bank_account_id, p.paid_at AS occurred_on, p.paid_amount AS amount,
    p.payment_id, NULL::bigint AS reversal_id
FROM accounts_receivable_payments p JOIN accounts_receivable a USING (account_receivable_id)
UNION ALL
SELECT 'receivable:reversal:' || r.reversal_id, 'ACTUAL', 'RECEIVABLE',
    a.account_receivable_id, a.order_id, a.customer_id, a.financial_category_id,
    p.bank_account_id, r.reversed_at, -p.paid_amount, p.payment_id, r.reversal_id
FROM accounts_receivable_reversals r JOIN accounts_receivable_payments p USING(payment_id)
JOIN accounts_receivable a USING(account_receivable_id)
UNION ALL
SELECT 'receivable:projection:' || a.account_receivable_id, 'PROJECTED', 'RECEIVABLE',
    a.account_receivable_id, a.order_id, a.customer_id, a.financial_category_id,
    NULL::bigint, a.due_date, (a.amount - COALESCE((SELECT sum(p.paid_amount)
        FROM accounts_receivable_payments p WHERE p.account_receivable_id=a.account_receivable_id
        AND NOT EXISTS (SELECT 1 FROM accounts_receivable_reversals r WHERE r.payment_id=p.payment_id)),0)),
    NULL::bigint, NULL::bigint
FROM accounts_receivable a WHERE a.status IN ('OPEN','PARTIAL','OVERDUE')
UNION ALL
SELECT 'payable:payment:' || p.payment_id AS id, 'ACTUAL'::varchar AS kind,
    'PAYABLE'::varchar AS direction, a.account_payable_id AS title_id,
    a.purchase_id AS document_id, a.supplier_id AS party_id, a.financial_category_id AS category_id,
    p.bank_account_id, p.paid_at AS occurred_on, -p.paid_amount AS amount,
    p.payment_id, NULL::bigint AS reversal_id
FROM accounts_payable_payments p JOIN accounts_payable a USING (account_payable_id)
UNION ALL
SELECT 'payable:reversal:' || r.reversal_id, 'ACTUAL', 'PAYABLE',
    a.account_payable_id, a.purchase_id, a.supplier_id, a.financial_category_id,
    p.bank_account_id, r.reversed_at, p.paid_amount, p.payment_id, r.reversal_id
FROM accounts_payable_reversals r JOIN accounts_payable_payments p USING(payment_id)
JOIN accounts_payable a USING(account_payable_id)
UNION ALL
SELECT 'payable:projection:' || a.account_payable_id, 'PROJECTED', 'PAYABLE',
    a.account_payable_id, a.purchase_id, a.supplier_id, a.financial_category_id,
    NULL::bigint, a.due_date, -(a.amount - COALESCE((SELECT sum(p.paid_amount)
        FROM accounts_payable_payments p WHERE p.account_payable_id=a.account_payable_id
        AND NOT EXISTS (SELECT 1 FROM accounts_payable_reversals r WHERE r.payment_id=p.payment_id)),0)),
    NULL::bigint, NULL::bigint
FROM accounts_payable a WHERE a.status IN ('OPEN','PARTIAL','OVERDUE');
