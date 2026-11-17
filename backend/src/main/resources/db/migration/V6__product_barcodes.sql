CREATE TABLE product_barcodes (
 barcode_id BIGSERIAL PRIMARY KEY,
 product_id BIGINT NOT NULL REFERENCES products(product_id),
 code VARCHAR(128) NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9._/-]{1,128}$'),
 type VARCHAR(10) NOT NULL CHECK (type IN ('EAN_GTIN','INTERNAL')),
 main_code BOOLEAN NOT NULL DEFAULT FALSE,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_by BIGINT NOT NULL REFERENCES users(user_id),
 created_at TIMESTAMP NOT NULL,
 updated_by BIGINT REFERENCES users(user_id),
 updated_at TIMESTAMP,
 CHECK (NOT main_code OR active)
);
CREATE UNIQUE INDEX uq_product_barcode_main ON product_barcodes(product_id) WHERE main_code;
CREATE INDEX ix_product_barcodes_product ON product_barcodes(product_id);
