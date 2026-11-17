import { useCatalogPage } from "../Products/useProductResource";
import { suppliers } from "../../services/suppliers";

export const emptyFilters = { query: "", searchField: "NAME", active: "" };

export default function useSuppliers(origin) {
  const filters = (() => {
    const restored = { ...emptyFilters };
    for (const key of Object.keys(restored))
      if (typeof origin?.filters?.[key] === "string") restored[key] = origin.filters[key];
    if (!["", "true", "false"].includes(restored.active)) restored.active = "";
    if (!["NAME", "DOCUMENT", "CONTACT"].includes(restored.searchField))
      restored.searchField = "NAME";

    return restored;
  })();
  return useCatalogPage(suppliers.search, filters, origin);
}
