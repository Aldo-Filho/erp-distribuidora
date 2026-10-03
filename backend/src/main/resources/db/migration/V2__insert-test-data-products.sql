-- V3__insert-test-data.sql


-- =====================================================================
-- CATEGORIAS
-- =====================================================================

INSERT INTO categories (name) VALUES
                                  ('Eletrônicos'),
                                  ('Vestuário'),
                                  ('Papelaria'),
                                  ('Ferramentas'),
                                  ('Casa e Cozinha'),
                                  ('Automotivo'),
                                  ('Informática');


-- =====================================================================
-- MARCAS
-- =====================================================================

INSERT INTO brands (name) VALUES
                              ('Samsung'),
                              ('Nike'),
                              ('Tramontina'),
                              ('Faber-Castell'),
                              ('Bosch'),
                              ('Intel'),
                              ('Multilaser'),
                              ('Wurth');


-- =====================================================================
-- PRODUTOS
-- =====================================================================

INSERT INTO products (
    name,
    sku,
    category_id,
    brand_id,
    cost,
    price,
    weight_kg,
    color,
    dimension_x,
    dimension_y,
    dimension_z,
    size,
    active
) VALUES

      (
          'Smartphone Galaxy A54',
          'SKU-ELET-001',
          (SELECT category_id FROM categories WHERE name = 'Eletrônicos'),
          (SELECT brand_id FROM brands WHERE name = 'Samsung'),
          900.00,
          1499.90,
          0.202,
          'Preto',
          15.8,
          7.7,
          0.8,
          NULL,
          TRUE
      ),

      (
          'Fone de Ouvido Bluetooth',
          'SKU-ELET-002',
          (SELECT category_id FROM categories WHERE name = 'Eletrônicos'),
          (SELECT brand_id FROM brands WHERE name = 'Samsung'),
          60.00,
          129.90,
          0.050,
          'Branco',
          6.0,
          6.0,
          3.0,
          NULL,
          TRUE
      ),

      (
          'Camiseta Dry-Fit',
          'SKU-VEST-001',
          (SELECT category_id FROM categories WHERE name = 'Vestuário'),
          (SELECT brand_id FROM brands WHERE name = 'Nike'),
          25.00,
          79.90,
          0.150,
          'Azul',
          NULL,
          NULL,
          NULL,
          'M',
          TRUE
      ),

      (
          'Tênis Corrida Revolution',
          'SKU-VEST-002',
          (SELECT category_id FROM categories WHERE name = 'Vestuário'),
          (SELECT brand_id FROM brands WHERE name = 'Nike'),
          110.00,
          299.90,
          0.600,
          'Cinza',
          30.0,
          12.0,
          11.0,
          '42',
          TRUE
      ),

      (
          'Caneta Esferográfica Azul',
          'SKU-PAP-001',
          (SELECT category_id FROM categories WHERE name = 'Papelaria'),
          (SELECT brand_id FROM brands WHERE name = 'Faber-Castell'),
          0.80,
          2.50,
          0.010,
          'Azul',
          14.0,
          1.0,
          1.0,
          NULL,
          TRUE
      ),

      (
          'Estojo Escolar',
          'SKU-PAP-002',
          (SELECT category_id FROM categories WHERE name = 'Papelaria'),
          (SELECT brand_id FROM brands WHERE name = 'Faber-Castell'),
          8.00,
          24.90,
          0.120,
          'Rosa',
          20.0,
          8.0,
          5.0,
          NULL,
          TRUE
      ),

      (
          'Panela de Pressão 4.5L',
          'SKU-CASA-001',
          (SELECT category_id FROM categories WHERE name = 'Casa e Cozinha'),
          (SELECT brand_id FROM brands WHERE name = 'Tramontina'),
          85.00,
          189.90,
          1.800,
          'Prata',
          22.0,
          22.0,
          18.0,
          NULL,
          TRUE
      ),

      (
          'Furadeira de Impacto',
          'SKU-FERR-001',
          (SELECT category_id FROM categories WHERE name = 'Ferramentas'),
          (SELECT brand_id FROM brands WHERE name = 'Bosch'),
          220.00,
          449.90,
          1.500,
          'Verde',
          25.0,
          22.0,
          8.0,
          NULL,
          TRUE
      ),

      (
          'Processador Intel Core i5',
          'SKU-INF-001',
          (SELECT category_id FROM categories WHERE name = 'Informática'),
          (SELECT brand_id FROM brands WHERE name = 'Intel'),
          650.00,
          999.90,
          0.450,
          'Prata',
          12.0,
          8.0,
          7.0,
          NULL,
          TRUE
      ),

      (
          'Teclado USB',
          'SKU-INF-002',
          (SELECT category_id FROM categories WHERE name = 'Informática'),
          (SELECT brand_id FROM brands WHERE name = 'Multilaser'),
          35.00,
          79.90,
          0.550,
          'Preto',
          45.0,
          15.0,
          3.0,
          NULL,
          TRUE
      ),

      (
          'Kit Chave de Fenda',
          'SKU-FERR-002',
          (SELECT category_id FROM categories WHERE name = 'Ferramentas'),
          (SELECT brand_id FROM brands WHERE name = 'Bosch'),
          45.00,
          99.90,
          0.700,
          'Azul',
          25.0,
          15.0,
          5.0,
          NULL,
          TRUE
      ),

      (
          'Jogo de Pastilhas de Freio',
          'SKU-AUTO-001',
          (SELECT category_id FROM categories WHERE name = 'Automotivo'),
          (SELECT brand_id FROM brands WHERE name = 'Wurth'),
          120.00,
          249.90,
          1.200,
          'Cinza',
          18.0,
          12.0,
          8.0,
          NULL,
          TRUE
      ),

      (
          'Produto Descontinuado',
          'SKU-DESC-001',
          NULL,
          (SELECT brand_id FROM brands WHERE name = 'Samsung'),
          50.00,
          99.90,
          NULL,
          NULL,
          NULL,
          NULL,
          NULL,
          NULL,
          FALSE
      );


-- =====================================================================
-- DEPÓSITOS
-- =====================================================================

INSERT INTO warehouses (name, description) VALUES
                                               ('Depósito Principal', 'Depósito principal da distribuidora'),
                                               ('Depósito Secundário', 'Depósito para armazenamento de produtos'),
                                               ('Centro de Distribuição', 'Centro de distribuição para expedição de pedidos');


-- =====================================================================
-- ENDEREÇOS DOS DEPÓSITOS
-- =====================================================================

INSERT INTO warehouse_addresses (
    warehouse_id,
    state,
    city,
    street,
    neighborhood,
    number,
    complement,
    zip_code
) VALUES

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          'PE',
          'Bezerros',
          'Rua Principal',
          'Centro',
          '100',
          'Galpão A',
          '55660-000'
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Secundário'),
          'PE',
          'Caruaru',
          'Avenida Brasil',
          'Maurício de Nassau',
          '500',
          'Galpão B',
          '55012-000'
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Centro de Distribuição'),
          'PE',
          'Recife',
          'Avenida Norte',
          'Arruda',
          '1200',
          'Galpão Central',
          '52110-000'
      );


-- =====================================================================
-- ESTOQUE
-- =====================================================================

INSERT INTO stock_items (
    warehouse_id,
    product_id,
    quantity,
    reserved_quantity,
    min_quantity,
    max_quantity
) VALUES

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-ELET-001'),
          50,
          5,
          10,
          100
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-ELET-002'),
          120,
          10,
          30,
          200
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-VEST-001'),
          80,
          8,
          20,
          150
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-VEST-002'),
          35,
          5,
          10,
          80
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-PAP-001'),
          500,
          30,
          100,
          800
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-PAP-002'),
          70,
          5,
          15,
          120
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-CASA-001'),
          25,
          2,
          8,
          50
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-FERR-001'),
          40,
          4,
          10,
          70
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-INF-001'),
          30,
          3,
          8,
          60
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Principal'),
          (SELECT product_id FROM products WHERE sku = 'SKU-INF-002'),
          100,
          10,
          20,
          180
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Secundário'),
          (SELECT product_id FROM products WHERE sku = 'SKU-ELET-001'),
          20,
          2,
          5,
          50
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Secundário'),
          (SELECT product_id FROM products WHERE sku = 'SKU-FERR-002'),
          60,
          5,
          15,
          100
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Depósito Secundário'),
          (SELECT product_id FROM products WHERE sku = 'SKU-AUTO-001'),
          45,
          5,
          10,
          80
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Centro de Distribuição'),
          (SELECT product_id FROM products WHERE sku = 'SKU-ELET-001'),
          100,
          15,
          30,
          200
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Centro de Distribuição'),
          (SELECT product_id FROM products WHERE sku = 'SKU-ELET-002'),
          200,
          20,
          50,
          300
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Centro de Distribuição'),
          (SELECT product_id FROM products WHERE sku = 'SKU-FERR-001'),
          75,
          10,
          20,
          120
      ),

      (
          (SELECT warehouse_id FROM warehouses WHERE name = 'Centro de Distribuição'),
          (SELECT product_id FROM products WHERE sku = 'SKU-AUTO-001'),
          90,
          8,
          20,
          150
      );