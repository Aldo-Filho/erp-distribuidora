import assert from "node:assert/strict";
import test from "node:test";
import { buildStockPayload, createProductWithStock } from "../src/pages/Products/productStock.js";

const form = {
  warehouseId: "2",
  quantity: "10",
  reservedQuantity: "3",
  minQuantity: "5",
  maxQuantity: "20",
};

test("estoque inicial é opcional e converte os valores para números", () => {
  assert.equal(buildStockPayload({ ...form, warehouseId: "" }), null);
  assert.deepEqual(buildStockPayload(form), {
    warehouseId: 2,
    quantity: 10,
    reservedQuantity: 3,
    minQuantity: 5,
    maxQuantity: 20,
  });
  assert.equal(buildStockPayload({ ...form, quantity: "0", reservedQuantity: "0" }).quantity, 0);
});

test("rejeita quantidades vazias, negativas, fracionadas e maiores que o limite da API", () => {
  for (const field of ["quantity", "reservedQuantity", "minQuantity", "maxQuantity"]) {
    for (const value of ["", " ", "-1", "1.5", "abc", "2147483648"]) {
      assert.throws(() => buildStockPayload({ ...form, [field]: value }), /inteiros/);
    }
  }
  assert.throws(() => buildStockPayload({ ...form, reservedQuantity: "11" }), /reservada/);
  assert.throws(() => buildStockPayload({ ...form, maxQuantity: "4" }), /mínimo/);
  assert.throws(() => buildStockPayload({ ...form, warehouseId: "abc" }), /armazém válido/);
});

test("cadastra o estoque usando o ID retornado pelo cadastro do produto", async () => {
  const calls = [];
  let createdId;
  await createProductWithStock({
    payload: { name: "Produto", sku: "P-1" },
    stock: buildStockPayload(form),
    onCreated: (id) => {
      createdId = id;
    },
    request: async (path, options) => {
      calls.push([path, options.method, JSON.parse(options.body)]);
      return { id: 42 };
    },
  });
  assert.equal(createdId, 42);
  assert.deepEqual(calls, [
    ["/product", "POST", { name: "Produto", sku: "P-1" }],
    [
      "/stockItem",
      "POST",
      {
        warehouseId: 2,
        productId: 42,
        quantity: 10,
        reservedQuantity: 3,
        minQuantity: 5,
        maxQuantity: 20,
      },
    ],
  ]);
});

test("permite repetir o cadastro de estoque sem duplicar o produto após falha", async () => {
  const calls = [];
  let productId;
  let failStock = true;
  const options = {
    payload: { sku: "P-1" },
    stock: buildStockPayload(form),
    onCreated: (id) => {
      productId = id;
    },
    request: async (path) => {
      calls.push(path);
      if (path === "/stockItem" && failStock) throw new Error("Falha na conexão");
      return { id: 42 };
    },
  };
  await assert.rejects(
    createProductWithStock(options),
    /produto foi criado, mas o estoque não foi salvo/,
  );
  assert.equal(productId, 42);
  failStock = false;
  await createProductWithStock({ ...options, productId });
  assert.deepEqual(calls, ["/product", "/stockItem", "/stockItem"]);
});

test("não tenta criar estoque quando o produto falha ou o estoque não foi informado", async () => {
  const calls = [];
  const options = {
    payload: {},
    stock: buildStockPayload(form),
    onCreated: () => assert.fail("Produto não foi criado"),
    request: async (path) => {
      calls.push(path);
      throw new Error("SKU duplicado");
    },
  };
  await assert.rejects(createProductWithStock(options), /SKU duplicado/);
  assert.deepEqual(calls, ["/product"]);
  calls.length = 0;
  await createProductWithStock({
    ...options,
    stock: null,
    onCreated: () => {},
    request: async (path) => {
      calls.push(path);
      return { id: 1 };
    },
  });
  assert.deepEqual(calls, ["/product"]);
});
