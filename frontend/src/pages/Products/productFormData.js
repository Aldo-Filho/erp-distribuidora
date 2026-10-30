export const units = {
  UN: "Unidade",
  KG: "Quilograma",
  G: "Grama",
  L: "Litro",
  ML: "Mililitro",
  M: "Metro",
};
export const characteristics = {
  COLOR: "Cor",
  SIZE: "Tamanho",
  VOLTAGE: "Voltagem",
  POWER: "Potência",
  CAPACITY: "Capacidade",
  MODEL: "Modelo",
};
export const money = (value) =>
  value == null
    ? "—"
    : Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
export const quantity = (value) =>
  value == null ? "—" : Number(value).toLocaleString("pt-BR", { maximumFractionDigits: 1 });
export const textNumber = (value) => (value == null ? "" : String(value).replace(".", ","));

// Display-only preview: integer cents × integer tenths, matching server HALF_UP.
// No stock or commercial payload is derived from this preview.
export function presentationPrice(price, factor) {
  if (price == null || factor == null) return "—";
  const scaled = (value, places) => {
    const [whole, fraction = ""] = String(value).split(".");
    return BigInt(whole + fraction.padEnd(places, "0"));
  };
  const cents = (scaled(price, 2) * scaled(factor, 1) + 5n) / 10n;
  if (cents > 999999999999n) return "Acima do limite de venda";
  return money(`${cents / 100n}.${String(cents % 100n).padStart(2, "0")}`);
}

// Validate the original text before Number; trailing zeros are insignificant.
export function decimal(
  text,
  label,
  { places = 2, optional = false, positive = false, max = 9999999999.99 } = {},
) {
  const raw = String(text ?? "").trim();
  if (!raw && optional) return null;
  if (!/^\d+(?:[.,]\d+)?$/.test(raw))
    throw new Error(`${label}: informe um número não negativo, sem separador de milhar.`);
  const normalized = raw.replace(",", ".");
  const fraction = (normalized.split(".")[1] || "").replace(/0+$/, "");
  if (fraction.length > places)
    throw new Error(`${label}: use no máximo ${places} casa(s) decimal(is).`);
  const value = Number(normalized);
  if (!Number.isFinite(value) || value > max || (positive && value <= 0))
    throw new Error(`${label}: valor fora da faixa permitida.`);
  return value;
}
export function baseNumber(text, unit, label, optional = false, positive = false) {
  if (!unit) throw new Error("Confirme a unidade base do produto antes de informar quantidades.");
  return decimal(text, label, {
    places: unit === "UN" ? 0 : 1,
    optional,
    positive,
    max: 9999999999999.9,
  });
}

export function itemValues(item) {
  return {
    cost: textNumber(item?.cost),
    price: textNumber(item?.price),
    weightKg: textNumber(item?.weightKg),
    dimensionX: textNumber(item?.dimensionX),
    dimensionY: textNumber(item?.dimensionY),
    dimensionZ: textNumber(item?.dimensionZ),
    color: item?.color || "",
    size: item?.size || "",
    attributes: { ...item?.attributes },
    withStock: false,
    stock: {
      warehouseId: "",
      quantity: "0",
      reservedQuantity: "0",
      minQuantity: "0",
      maxQuantity: "",
    },
  };
}
export function itemPayload(value, catalog, item, canStock) {
  const body = {};
  for (const field of ["cost", "price", "weightKg", "dimensionX", "dimensionY", "dimensionZ"]) {
    const labels = {
      cost: "Custo de referência",
      price: "Preço base",
      weightKg: "Peso",
      dimensionX: "Dimensão X",
      dimensionY: "Dimensão Y",
      dimensionZ: "Dimensão Z",
    };
    const optional = !["cost", "price"].includes(field);
    const next = decimal(value[field], labels[field], {
      optional,
      places: field === "weightKg" ? 3 : 2,
      max:
        field === "weightKg"
          ? 9999999.999
          : field.startsWith("dimension")
            ? 99999999.99
            : 9999999999.99,
    });
    if (!item) {
      if (next != null) body[field] = next;
    } else if (next !== (item[field] ?? null)) {
      if (next == null) body[`remove${field[0].toUpperCase()}${field.slice(1)}`] = true;
      else body[field] = next;
    }
  }
  if (catalog.type === "VARIANT") {
    const attrs = Object.fromEntries(
      catalog.characteristics.map((field) => [field, (value.attributes[field] || "").trim()]),
    );
    if (Object.values(attrs).some((text) => !text))
      throw new Error("Preencha todas as características da variação.");
    if (!item || Object.keys(attrs).some((field) => attrs[field] !== item.attributes?.[field]))
      body.attributes = attrs;
  } else {
    for (const field of ["color", "size"]) {
      if (!item || value[field] !== (item[field] || "")) body[field] = value[field].trim();
    }
  }
  if (!item && value.withStock && !canStock) {
    throw new Error(
      "O rascunho inclui estoque inicial sem as permissões atuais. Desmarque essa inclusão para cadastrar somente o produto.",
    );
  }
  if (!item && value.withStock && canStock) {
    const stock = value.stock;
    if (!stock.warehouseId) throw new Error("Escolha o depósito do estoque inicial.");
    body.stock = { warehouseId: Number(stock.warehouseId) };
    for (const field of ["quantity", "reservedQuantity", "minQuantity", "maxQuantity"]) {
      const next = baseNumber(
        stock[field],
        catalog.unit,
        "Estoque inicial",
        field === "maxQuantity",
      );
      if (next != null) body.stock[field] = next;
    }
    if (body.stock.reservedQuantity !== 0)
      throw new Error("O reservado inicial deve ser zero. As reservas são geradas pelos pedidos.");
    if (body.stock.maxQuantity != null && body.stock.maxQuantity < body.stock.minQuantity)
      throw new Error("O máximo não pode ser menor que o mínimo.");
  }
  return body;
}
export function variantIdentity(attributes) {
  return JSON.stringify(
    Object.entries(attributes)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, value]) => [key, value.trim().toLowerCase()]),
  );
}
export const inactiveCatalogMessage =
  "Ao inativar o produto principal, todas as suas variações também ficarão inativas e indisponíveis para novas compras e vendas. Os saldos e o histórico serão preservados. Compromissos já confirmados continuam podendo ser concluídos ou cancelados.";
