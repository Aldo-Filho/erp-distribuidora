function normalizeText(value) {
  return String(value ?? "")
    .trim()
    .toLocaleLowerCase("pt-BR");
}

// Unifica os separadores sem completar os dígitos que o usuário ainda não digitou.
function normalizePriceInput(value) {
  let amount = String(value)
    .trim()
    .replace(/^R\$\s*/i, "");
  if (/^\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?$/.test(amount)) {
    amount = amount.replaceAll(".", "");
  }
  return amount.replace(",", ".");
}

// Aceita reais com vírgula, ponto decimal ou separador de milhares brasileiro.
export function parsePriceCents(value) {
  const amount = normalizePriceInput(value);
  if (!/^\d+(?:\.\d{1,2})?$/.test(amount)) return null;
  const [integer, decimal = ""] = amount.split(".");
  const cents = Number(integer) * 100 + Number(decimal.padEnd(2, "0"));
  return Number.isSafeInteger(cents) ? cents : null;
}

export function filterProducts(products, filters) {
  const search = normalizeText(filters.search);
  const searchedPrice = filters.searchBy === "price" && search ? normalizePriceInput(search) : null;

  return products
    .filter((product) => {
      if (filters.categoryId && String(product.category?.id) !== filters.categoryId) return false;
      if (filters.brandId && String(product.brand?.id) !== filters.brandId) return false;
      if (filters.active !== "" && String(product.active) !== filters.active) return false;
      if (!search) return true;
      if (filters.searchBy === "price") {
        if (searchedPrice === null || parsePriceCents(search) === null || product.price == null)
          return false;
        const price = Number(product.price);
        return Number.isFinite(price) && price.toFixed(2).includes(searchedPrice);
      }
      return normalizeText(product[filters.searchBy]).includes(search);
    })
    .sort((first, second) =>
      normalizeText(first.name).localeCompare(normalizeText(second.name), "pt-BR"),
    );
}
