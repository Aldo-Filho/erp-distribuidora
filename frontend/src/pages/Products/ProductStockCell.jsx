import { useCallback, useId, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { productStock } from "../../services/productStock";
import { useProductResource } from "./useProductResource";
import ProductStockPopover from "./ProductStockPopover";
import ProductStockDialog from "./ProductStockDialog";

// Sum integer tenths so decimal quantities remain exact in the display.
function tenths(value) {
  const [whole, fraction = ""] = String(value).split(".");
  return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
}

function formatTenths(value) {
  return `${(value / 10n).toLocaleString("pt-BR")}${value % 10n ? `,${value % 10n}` : ""}`;
}

export default function ProductStockCell({ item, onChanged }) {
  const { has } = useAuth();
  const [open, setOpen] = useState(false);
  const [managing, setManaging] = useState(false);
  const trigger = useRef(null);
  const id = useId();
  const close = useCallback((restore = true) => {
    setOpen(false);
    if (restore) trigger.current?.focus({ preventScroll: true });
  }, []);
  const state = useProductResource(
    (options) => productStock.search(item.id, options),
    [item.id],
    has("stock.read"),
  );
  const totals = state.data?.reduce(
    (total, stock) => ({
      balance: total.balance + tenths(stock.quantity),
      reserved: total.reserved + tenths(stock.reservedQuantity),
    }),
    { balance: 0n, reserved: 0n },
  );
  if (!has("stock.read")) return null;
  return (
    <>
      <button
        ref={trigger}
        type="button"
        className="product-stock-summary"
        disabled={state.loading}
        aria-label={`Consultar estoque de ${item.sku}`}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        onClick={state.error ? state.reload : () => setOpen((previous) => !previous)}
      >
        <ChevronDown size={15} aria-hidden="true" />
        {state.loading ? "Carregando…" : state.error ? "Recarregar saldo" : totals && (
          <span className="product-stock-figures">
            <span>
              <small>Estoque</small>
              <strong>{formatTenths(totals.balance)}</strong>
            </span>
            <span className="product-stock-reserved">
              <small>Reservado</small>
              <strong>{formatTenths(totals.reserved)}</strong>
            </span>
            <span className="product-stock-available">
              <small>Disponível</small>
              <strong>{formatTenths(totals.balance - totals.reserved)}</strong>
            </span>
            {item.unit && <span className="product-stock-unit">{item.unit}</span>}
          </span>
        )}
      </button>
      {open && state.data && (
        <ProductStockPopover
          id={id}
          item={item}
          stocks={state.data}
          anchor={trigger}
          onClose={close}
          onManage={() => {
            close();
            setManaging(true);
          }}
        />
      )}
      {managing && (
        <ProductStockDialog
          item={item}
          onClose={() => setManaging(false)}
          onChanged={() => {
            state.reload();
            onChanged?.();
          }}
        />
      )}
    </>
  );
}
