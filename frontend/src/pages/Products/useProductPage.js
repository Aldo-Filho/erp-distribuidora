import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";

export default function useProductPage(id) {
  return useProductResource(
    async (options) => {
      const catalog = await products.findOne(id, options);
      if (!catalog) return null;
      const items = catalog.type === "SIMPLE" ? await products.items(id, {}, 0, 2, options) : null;
      if (items && (items.totalElements !== 1 || items.content.length !== 1))
        throw new Error("O produto simples não possui um único SKU disponível.");
      return { catalog, item: items?.content[0] };
    },
    [id],
    Boolean(id),
  );
}
