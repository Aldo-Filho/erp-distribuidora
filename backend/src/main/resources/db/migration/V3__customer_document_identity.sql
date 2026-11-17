-- Preserve legacy documents; refuse normalized duplicates rather than merging customers.
-- Existing invalid documents must be corrected explicitly by the operator.
CREATE UNIQUE INDEX uk_customer_tax_id_normalized
    ON customers (upper(regexp_replace(tax_id, '[^0-9A-Za-z]', '', 'g')));
