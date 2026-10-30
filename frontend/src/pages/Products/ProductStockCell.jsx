import { ChevronDown } from "lucide-react";
import { productStock } from "../../services/productStock";
import { useProductResource } from "./useProductResource";

// Sum integer tenths so decimal quantities remain exact in the display.
function tenths(value) {
  const [whole, fraction = ""] = String(value).split(".");
  return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
}

function formatTenths(value) {
  return `${(value / 10n).toLocaleString("pt-BR")}${value % 10n ? `,${value % 10n}` : ""}`;
}

export default function ProductStockCell({ item, onOpen }) {
  const state = useProductResource((options) => productStock.search(item.id, options), [item.id]);
  const totals = state.data?.reduce(
    (total, stock) => ({
      balance: total.balance + tenths(stock.quantity),
      reserved: total.reserved + tenths(stock.reservedQuantity),
    }),
    { balance: 0n, reserved: 0n },
  );
  return (
    <button type="button" className="product-stock-summary"
      disabled={state.loading} aria-label={`Consultar estoque de ${item.sku}`}
      aria-haspopup="dialog" onClick={state.error ? state.reload : onOpen}>
      <ChevronDown size={15} aria-hidden="true" />
      {state.loading ? "Carregando…" : state.error ? "Recarregar saldo" : totals && (
        <span className="product-stock-figures">
          <span><small>Estoque</small><strong>{formatTenths(totals.balance)}</strong></span>
          <span className="product-stock-reserved">
            <small>Reservado</small><strong>{formatTenths(totals.reserved)}</strong>
          </span>
          <span className="product-stock-available">
            <small>Disponível</small><strong>{formatTenths(totals.balance - totals.reserved)}</strong>
          </span>
          {item.unit && <span className="product-stock-unit">{item.unit}</span>}
        </span>
      )}
    </button>
  );
}
