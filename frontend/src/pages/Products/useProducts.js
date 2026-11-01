import { products } from "../../services/products";
import { useCatalogPage } from "./useProductResource";

export const emptyFilters = { query: "", brandId: "", categoryId: "", active: "" };
export default function useProducts(origin) {
  const filters = { ...emptyFilters };
  for (const key of Object.keys(filters)) {
    if (typeof origin?.filters?.[key] === "string") filters[key] = origin.filters[key];
  }
  if (!["", "true", "false"].includes(filters.active)) filters.active = "";
  return useCatalogPage(products.search, filters, origin);
}
