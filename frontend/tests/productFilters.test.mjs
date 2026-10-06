import assert from "node:assert/strict";
import test from "node:test";
import { filterProducts, parsePriceCents } from "../src/pages/Products/productFilters.js";

const products = [
  { id: 1, name: "Cabo USB", sku: "ABC-01", price: 49.9, brand: { id: 1 }, category: { id: 2 }, active: true },
  { id: 2, name: "Adaptador ABC", sku: "USB-02", price: "149.90", brand: { id: 3 }, category: { id: 2 }, active: false },
  { id: 3, name: "Monitor", sku: "MON-03", price: 1234.56, brand: { id: 1 }, category: null, active: true },
  { id: 4, name: "Brinde", sku: "ZERO-04", price: 0, brand: { id: 1 }, category: { id: 2 }, active: true },
];
const defaults = { search: "", searchBy: "name", categoryId: "", brandId: "", active: "" };
const results = (overrides) => filterProducts(products, { ...defaults, ...overrides }).map((product) => product.id);

test("nome e SKU pesquisam apenas o campo selecionado", () => {
  assert.deepEqual(results({ search: "  usb  " }), [1]);
  assert.deepEqual(results({ search: "usb", searchBy: "sku" }), [2]);
  assert.deepEqual(results({ search: "abc" }), [2]);
  assert.deepEqual(results({ search: "abc", searchBy: "sku" }), [1]);
});

test("valor pesquisa trechos do preço com ponto, vírgula ou R$", () => {
  for (const search of ["49,90", "49.90", "49,9", "R$ 49,90"]) {
    assert.deepEqual(results({ search, searchBy: "price" }), [2, 1]);
  }
  assert.deepEqual(results({ search: "49", searchBy: "price" }), [2, 1]);
  assert.deepEqual(results({ search: "1.234,56", searchBy: "price" }), [3]);
  assert.deepEqual(results({ search: "1234.56", searchBy: "price" }), [3]);
  assert.deepEqual(results({ search: "0", searchBy: "price" }), [2, 4, 1]);
});

test("valores inválidos não viram preços válidos", () => {
  for (const search of ["abc", "49abc90", "49,900", "-49", "49..90", "R$", ",", "Infinity"]) {
    assert.equal(parsePriceCents(search), null);
    assert.deepEqual(results({ search, searchBy: "price" }), []);
  }
  assert.equal(parsePriceCents("1.234"), 123400);
  assert.equal(parsePriceCents("1.234.567,89"), 123456789);
  assert.equal(parsePriceCents("9999999999999999999"), null);
});

test("campo vazio exibe todos; categoria, marca e status combinam com a busca", () => {
  assert.deepEqual(results({ searchBy: "price" }), [2, 4, 1, 3]);
  assert.deepEqual(results({ search: "abc", searchBy: "sku", brandId: "3" }), []);
  assert.deepEqual(results({ search: "49,90", searchBy: "price", categoryId: "2", brandId: "1", active: "true" }), [1]);
  assert.deepEqual(results({ search: "abc", active: "false" }), [2]);
  assert.deepEqual(results({ categoryId: "2" }), [2, 4, 1]);
});

test("filtragem mantém a coleção original e tolera campos ausentes", () => {
  const originalIds = products.map((product) => product.id);
  results({});
  assert.deepEqual(products.map((product) => product.id), originalIds);
  assert.deepEqual(filterProducts([{ id: 5 }], { ...defaults, search: "x", searchBy: "sku" }), []);
  assert.deepEqual(filterProducts([{ id: 5 }], { ...defaults, search: "0", searchBy: "price" }), []);
});
