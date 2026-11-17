-- Documento demonstrativo separado do dominio fiscal legado/real.
-- Sem backfill: vendas ja despachadas nao recebem uma emissao inventada.
CREATE TABLE demonstrative_invoices (
    demonstrative_invoice_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(order_id),
    issued_by BIGINT NOT NULL REFERENCES users(user_id),
    issued_at TIMESTAMPTZ NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1 CHECK (schema_version = 1),
    snapshot JSONB NOT NULL CHECK (jsonb_typeof(snapshot) = 'object')
);
COMMENT ON TABLE demonstrative_invoices IS 'DEMONSTRATIVO - SEM VALIDADE FISCAL. Sem autorizacao SEFAZ.';
-- Apenas INSERT/SELECT no modulo: nao alterar documentos emitidos com os cadastros atuais.
