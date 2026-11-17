import { useCatalogPage } from "../Products/useProductResource";
import { customers } from "../../services/customers";

export const emptyFilters = { query: "", searchField: "NAME", personType: "", active: "" };

export default function useCustomers(origin) {
  const filters = (() => {
    const restored = { ...emptyFilters };
    for (const key of Object.keys(restored))
      if (typeof origin?.filters?.[key] === "string") restored[key] = origin.filters[key];
    if (!["", "true", "false"].includes(restored.active)) restored.active = "";
    if (!["NAME", "DOCUMENT", "CONTACT"].includes(restored.searchField))
      restored.searchField = "NAME";
    if (!["", "PF", "PJ"].includes(restored.personType)) restored.personType = "";
    return restored;
  })();
  return useCatalogPage(customers.search, filters, origin);
}
