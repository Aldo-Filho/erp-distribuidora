import { useState } from "react";
import { History, Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { productStock } from "../../services/productStock";
import { useProductResource } from "./useProductResource";
import { ProductLoad } from "./ProductControls";
import { quantity } from "./productFormData";
import { StockConfiguration, StockMovement } from "../Operations/StockActions";
import StockHistory from "../Operations/StockHistory";
import { available } from "../Operations/StockPage";

export default function ProductStockPanel({ item, onChanged }) {
  const { has } = useAuth();
  const [flow, setFlow] = useState(null);
  const [notice, setNotice] = useState("");
  const state = useProductResource((options) => productStock.search(item.id, options), [item.id]);
  const saved = () => {
    setFlow(null);
    setNotice("Estoque atualizado.");
    state.reload();
    onChanged?.();
  };
  return (
    <section className="product-stock-panel" aria-label={`Estoque de ${item.sku}`}>
      <div className="product-toolbar">
        <h3>Estoque por depósito</h3>
        <button type="button" className="secondary customer-icon-label"
          onClick={() => setFlow({ type: "history" })}>
          <History size={17} /> Histórico do produto
        </button>
        {has("stock.configure") && has("warehouses.read") && (
          <button type="button" className="customer-icon-label"
            onClick={() => setFlow({ type: "configure" })}>
            <Plus size={17} /> Configurar depósito
          </button>
        )}
      </div>
      {notice && <p role="status" className="success">{notice}</p>}
      <ProductLoad state={state} empty="Nenhum estoque configurado para este produto.">
        <div className="product-stock-depots">
          {state.data?.map((stock) => (
            <section key={stock.id} className="product-stock-depot">
              <h4>{stock.warehouse.name}</h4>
              <dl className="customer-details">
                {[
                  ["Físico", quantity(stock.quantity)],
                  ["Reservado", quantity(stock.reservedQuantity)],
                  ["Disponível", available(stock)],
                  ["Mínimo", quantity(stock.minQuantity)],
                  ["Máximo", stock.maxQuantity == null ? "Não informado" : quantity(stock.maxQuantity)],
                ].map(([label, value]) => (
                  <div key={label}><dt>{label}</dt><dd>{value}</dd></div>
                ))}
              </dl>
              <p className="field-hint">Quantidades em {item.unit || "unidade pendente"}.</p>
              <div className="product-toolbar">
                <button type="button" className="secondary"
                  onClick={() => setFlow({ type: "history", stock })}>Histórico</button>
                {has("stock.configure") && (
                  <button type="button" className="secondary"
                    onClick={() => setFlow({ type: "configure", stock })}>Configurar limites</button>
                )}
                {has("stock.adjust") && (
                  <button type="button"
                    onClick={() => setFlow({ type: "move", stock })}>Entrada ou ajuste</button>
                )}
              </div>
            </section>
          ))}
        </div>
      </ProductLoad>
      {flow?.type === "history" && (
        <StockHistory
          filter={flow.stock ? { productId: item.id, stockItemId: flow.stock.id } : { productId: item.id }}
          onClose={() => setFlow(null)} />
      )}
      {flow?.type === "configure" && has("stock.configure") && (
        <StockConfiguration product={item} stock={flow.stock} onSaved={saved} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "move" && has("stock.adjust") && (
        <StockMovement stock={flow.stock} onSaved={saved} onClose={() => setFlow(null)} />
      )}
    </section>
  );
}
