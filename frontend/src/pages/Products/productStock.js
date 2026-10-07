const quantityFields = [
  "quantity",
  "reservedQuantity",
  "minQuantity",
  "maxQuantity",
];

export function buildStockPayload(form) {
  if (!form.warehouseId) return null;
  const payload = { warehouseId: Number(form.warehouseId) };
  for (const field of quantityFields) {
    const value = Number(form[field]);
    if (
      String(form[field] ?? "").trim() === "" ||
      !Number.isInteger(value) ||
      value < 0 ||
      value > 2147483647
    ) {
      throw new Error(
        "Preencha as quantidades de estoque com números inteiros não negativos.",
      );
    }
    payload[field] = value;
  }
  if (!Number.isSafeInteger(payload.warehouseId) || payload.warehouseId <= 0) {
    throw new Error("Selecione um armazém válido.");
  }
  if (payload.reservedQuantity > payload.quantity) {
    throw new Error(
      "A quantidade reservada não pode exceder a quantidade em estoque.",
    );
  }
  if (payload.minQuantity > payload.maxQuantity) {
    throw new Error("O estoque mínimo não pode exceder o estoque máximo.");
  }
  return payload;
}

// O backend cadastra o produto e seu estoque opcional na mesma transação.
export async function createProductWithStock({ request, payload, stock }) {
  return request("/product", {
    method: "POST",
    body: JSON.stringify({ ...payload, stock }),
  });
}
