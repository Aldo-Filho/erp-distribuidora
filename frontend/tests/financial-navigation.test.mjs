import test from "node:test";
import assert from "node:assert/strict";
import { safeReturn, canAccessEntry, navigation } from "../src/routes/navigation.js";

test("financial readers can open titles and cash flow without accessing configuration", () => {
  const session = { permissionKeys: ["finance.read"] };
  for (const path of ["/finance/receivables", "/finance/payables", "/finance/cash-flow"]) {
    assert.equal(safeReturn(path, session), path);
    assert.equal(canAccessEntry(navigation.find((entry) => entry.path === path), session), true);
  }
  for (const path of ["/finance/categories", "/finance/bank-accounts"]) {
    assert.equal(canAccessEntry(navigation.find((entry) => entry.path === path), session), false);
    assert.notEqual(safeReturn(path, session), path);
  }
});

test("financial configuration and settlement permissions do not grant access to title totals", () => {
  for (const permission of ["finance.configure", "finance.settle", "finance.reverse"]) {
    const session = { permissionKeys: [permission] };
    for (const path of ["/finance/receivables", "/finance/payables", "/finance/cash-flow"]) {
      assert.equal(canAccessEntry(navigation.find((entry) => entry.path === path), session), false);
      assert.notEqual(safeReturn(path, session), path);
    }
  }
});

test("commercial document links require financial read access and a valid single document id", () => {
  const reader = { permissionKeys: ["finance.read"] };
  const seller = { permissionKeys: ["orders.read"] };
  for (const resource of ["receivables", "payables"]) {
    const path = `/finance/${resource}?documentId=123`;
    assert.equal(safeReturn(path, reader), path);
    assert.notEqual(safeReturn(path, seller), path);
    for (const query of ["documentId=0", "documentId=-1", "documentId=1.5",
      "documentId=9007199254740992", "documentId=1&documentId=2", "documentId=1&extra=2"]) {
      const invalid = `/finance/${resource}?${query}`;
      assert.notEqual(safeReturn(invalid, reader), invalid);
    }
  }
});
