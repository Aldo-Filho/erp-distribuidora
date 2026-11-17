import useAuth from "../../auth/useAuth";
import { productStock } from "../../services/productStock";
import { useProductResource } from "../Products/useProductResource";

const scaled = (value) => {
  const [whole, fraction = ""] = String(value ?? 0)
    .replace(",", ".")
    .split(".");
  return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0").slice(0, 1));
};
const display = (value) => {
  const magnitude = value < 0n ? -value : value;
  return `${value < 0n ? "-" : ""}${magnitude / 10n}${magnitude % 10n ? "," + String(magnitude % 10n) : ""}`;
};

export function useOrderStockCheck(items, enabled = true, revision) {
  const { has } = useAuth();
  const identity = items.map((item) => [
    item.id,
    item.product.id,
    item.warehouse.id,
    item.baseQuantity,
  ]);
  const state = useProductResource(
    async (options) => {
      const ids = [...new Set(items.map((item) => item.product.id))];
      const rows = (await Promise.all(ids.map((id) => productStock.search(id, options)))).flat();
      const groups = new Map();
      for (const item of items) {
        const key = `${item.product.id}:${item.warehouse.id}`;
        const group = groups.get(key) || { item, ids: [], requested: 0n };
        group.ids.push(item.id);
        group.requested += scaled(item.baseQuantity);
        groups.set(key, group);
      }
      return [...groups.values()].flatMap((group) => {
        const stock = rows.find(
          (row) =>
            row.product.id === group.item.product.id &&
            row.warehouse.id === group.item.warehouse.id,
        );
        const available = stock ? scaled(stock.quantity) - scaled(stock.reservedQuantity) : 0n;
        if (group.requested <= available) return [];
        return [
          {
            ...group,
            requested: display(group.requested),
            available: display(available),
            missing: display(group.requested - available),
          },
        ];
      });
    },
    [identity, enabled, revision],
    enabled && has("stock.read") && items.length > 0,
  );
  return {
    ...state,
    canRead: enabled && has("stock.read") && items.length > 0,
    shortages: state.data || [],
  };
}

export default function OrderStockCheck({ state, onAdjust }) {
  if (state.loading) return <p role="status">Conferindo disponibilidade nos depósitos…</p>;
  return (
    <>
      {state.canRead && (
        <button type="button" className="secondary" onClick={state.reload}>
          Reconsultar disponibilidade
        </button>
      )}
      {state.error && (
        <p className="product-warning" role="alert">
          Não foi possível consultar a disponibilidade.{" "}
          <button type="button" className="text-button" onClick={state.reload}>
            Consultar novamente
          </button>
        </p>
      )}
      {state.shortages.map(({ item, requested, available, missing }) => (
        <div
          className="product-warning"
          role="alert"
          key={`${item.product.id}:${item.warehouse.id}`}
        >
          <strong>
            {item.skuSnapshot || item.product.sku} · {item.warehouse.name}
          </strong>
          <p>
            Solicitado: {requested} · disponível: {available} · falta: {missing}{" "}
            {item.unitSnapshot || "BASE"}. A soma considera todas as apresentações deste SKU no
            depósito.
          </p>
          {onAdjust && (
            <button type="button" className="secondary" onClick={() => onAdjust(item)}>
              Ajustar quantidade ou depósito
            </button>
          )}
        </div>
      ))}
      {state.shortages.length > 0 && (
        <p className="field-hint">
          O pedido pode ficar pendente. Ajuste os itens antes de reservar.
        </p>
      )}
      <p className="field-hint">
        Disponibilidade indicativa. O estoque é validado novamente ao confirmar a reserva.
      </p>
    </>
  );
}
