import { products } from "../../services/products";
import { useCatalogPage } from "./useProductResource";

export const emptyFilters = { query: "", brandId: "", categoryId: "", active: "" };
export default function useProducts() {
  return useCatalogPage(products.search, emptyFilters);
}
