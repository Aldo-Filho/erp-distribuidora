import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import { money } from "./productFormData";
import ProductStockCell from "./ProductStockCell";

export default function ProductListRow({ catalog, canReadStock, onDetails, onStock, children }) {
  const simple = catalog.type === "SIMPLE";
  const state = useProductResource(
    async (options) => {
      const page = await products.items(catalog.id, {}, 0, 1, options);
      if (page.totalElements !== 1 || page.content.length !== 1) {
        throw new Error("O produto simples precisa ter exatamente um SKU. Consulte os detalhes.");
      }
      return page.content[0];
    },
    [catalog.id],
    simple,
  );
  return (
    <tr>
      <td className="product-list-sku">
        {!simple ? (
          <button type="button" className="text-button" onClick={onDetails}>
            Ver SKUs
          </button>
        ) : state.loading ? (
          <span role="status">Carregando…</span>
        ) : state.error ? (
          <button type="button" className="text-button" title={state.error} onClick={state.reload}>
            Recarregar SKU
          </button>
        ) : (
          state.data?.sku
        )}
      </td>
      <td>
        <strong>{catalog.name}</strong>
        <small>
          {simple ? "Simples" : "Com variações"} ·{" "}
          {catalog.unit || "Unidade pendente de conferência"}
        </small>
      </td>
      <td>{catalog.brand?.name || "—"}</td>
      <td>{catalog.category?.name || "Sem categoria"}</td>
      <td>
        <span className={`badge ${catalog.active ? "active" : ""}`}>
          {catalog.active ? "Ativo" : "Inativo"}
        </span>
      </td>
      <td>
        {!simple ? (
          <button type="button" className="text-button" onClick={onDetails}>
            Ver preços
          </button>
        ) : state.loading ? (
          "Carregando…"
        ) : (
          money(state.data?.price)
        )}
      </td>
      {canReadStock && (
        <td>
          {!simple ? (
            <button type="button" className="text-button" onClick={onStock}>
              Ver por variação
            </button>
          ) : state.data ? (
            <ProductStockCell item={state.data} />
          ) : state.loading ? (
            "Carregando…"
          ) : (
            "Indisponível"
          )}
        </td>
      )}
      {children}
    </tr>
  );
}
