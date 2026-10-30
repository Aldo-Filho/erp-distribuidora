import test from "node:test";
import assert from "node:assert/strict";
import { itemValues, itemPayload } from "../src/pages/Products/productFormData.js";

function initialStock(quantity = "1,5", reservedQuantity = "0") {
  return {
    ...itemValues(),
    cost: "0",
    price: "0",
    withStock: true,
    stock: { warehouseId: "1", quantity, reservedQuantity, minQuantity: "0", maxQuantity: "" },
  };
}

test("initial stock accepts comma decimals and preserves zero money and reservations", () => {
  const body = itemPayload(initialStock(), { type: "SIMPLE", unit: "KG" }, null, true);
  assert.equal(body.cost, 0);
  assert.equal(body.price, 0);
  assert.equal(body.stock.quantity, 1.5);
  assert.equal(body.stock.reservedQuantity, 0);
  assert.equal(body.stock.warehouseId, 1);
});

test("initial reservations are rejected before sending the registration", () => {
  assert.throws(
    () => itemPayload(initialStock("2", "1"), { type: "SIMPLE", unit: "UN" }, null, true),
    /reservado inicial deve ser zero/,
  );
});

test("stock precision is rejected without truncating the entered number", () => {
  assert.throws(
    () => itemPayload(initialStock("1,5"), { type: "SIMPLE", unit: "UN" }, null, true),
    /casa/,
  );
  assert.throws(
    () => itemPayload(initialStock("1,25"), { type: "SIMPLE", unit: "KG" }, null, true),
    /casa/,
  );
});
