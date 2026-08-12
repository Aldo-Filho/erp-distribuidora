-- V2__insert-test-data-products.sql

INSERT INTO categories (name) VALUES
('Eletrônicos'),
('Vestuário'),
('Papelaria'),
('Ferramentas'),
('Casa e Cozinha');

INSERT INTO brands (name) VALUES
('Samsung'),
('Nike'),
('Tramontina'),
('Faber-Castell'),
('Bosch');

INSERT INTO products (name, sku, category_id, brand_id, cost, price, weight_kg, color, dimension_x, dimension_y, dimension_z, size, active) VALUES
('Smartphone Galaxy A54', 'SKU-ELET-001', (SELECT category_id FROM categories WHERE name = 'Eletrônicos'), (SELECT brand_id FROM brands WHERE name = 'Samsung'), 900.00, 1499.90, 0.202, 'Preto', 15.8, 7.7, 0.8, NULL, TRUE),
('Fone de Ouvido Bluetooth', 'SKU-ELET-002', (SELECT category_id FROM categories WHERE name = 'Eletrônicos'), (SELECT brand_id FROM brands WHERE name = 'Samsung'), 60.00, 129.90, 0.05, 'Branco', 6.0, 6.0, 3.0, NULL, TRUE),
('Camiseta Dry-Fit', 'SKU-VEST-001', (SELECT category_id FROM categories WHERE name = 'Vestuário'), (SELECT brand_id FROM brands WHERE name = 'Nike'), 25.00, 79.90, 0.15, 'Azul', NULL, NULL, NULL, 'M', TRUE),
('Tênis Corrida Revolution', 'SKU-VEST-002', (SELECT category_id FROM categories WHERE name = 'Vestuário'), (SELECT brand_id FROM brands WHERE name = 'Nike'), 110.00, 299.90, 0.6, 'Cinza', 30.0, 12.0, 11.0, '42', TRUE),
('Caneta Esferográfica Azul', 'SKU-PAP-001', (SELECT category_id FROM categories WHERE name = 'Papelaria'), (SELECT brand_id FROM brands WHERE name = 'Faber-Castell'), 0.80, 2.50, 0.01, 'Azul', 14.0, 1.0, 1.0, NULL, TRUE),
('Estojo Escolar', 'SKU-PAP-002', (SELECT category_id FROM categories WHERE name = 'Papelaria'), (SELECT brand_id FROM brands WHERE name = 'Faber-Castell'), 8.00, 24.90, 0.12, 'Rosa', 20.0, 8.0, 5.0, NULL, TRUE),
('Panela de Pressão 4.5L', 'SKU-CASA-001', (SELECT category_id FROM categories WHERE name = 'Casa e Cozinha'), (SELECT brand_id FROM brands WHERE name = 'Tramontina'), 85.00, 189.90, 1.8, 'Prata', 22.0, 22.0, 18.0, NULL, TRUE),
('Furadeira de Impacto', 'SKU-FERR-001', (SELECT category_id FROM categories WHERE name = 'Ferramentas'), (SELECT brand_id FROM brands WHERE name = 'Bosch'), 220.00, 449.90, 1.5, 'Verde', 25.0, 22.0, 8.0, NULL, TRUE),
('Produto Descontinuado', 'SKU-DESC-001', NULL, (SELECT brand_id FROM brands WHERE name = 'Samsung'), 50.00, 99.90, NULL, NULL, NULL, NULL, NULL, NULL, FALSE);