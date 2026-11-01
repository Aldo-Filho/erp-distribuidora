import { Pencil, Trash2 } from "lucide-react";
import useAuth from "../../auth/useAuth";
import RecordMetadata from "../../components/record/RecordMetadata";
import { money, quantity } from "../Products/productFormData";

export default function PurchaseCart({ purchase, items = [], busy, onEdit, onRemove, children }) {
  const { has } = useAuth();
  return (
    <section className="panel order-entry-cart" id="purchase-cart" aria-label="Produtos da compra">
      <div className="order-entry-section-title">
        <h2>Produtos da compra</h2>
        <span className="field-hint">
          {items.length} {items.length === 1 ? "item" : "itens"}
        </span>
      </div>
      {!items.length && (
        <p className="empty">Escolha os produtos no catálogo para montar a compra.</p>
      )}
      {items.map((item) => (
        <article className="order-entry-cart-line" key={item.id}>
          <strong>{item.productNameSnapshot || item.product.name}</strong>
          <small>
            {item.skuSnapshot || item.product.sku} ·{" "}
            {item.presentationNameSnapshot ||
              item.supplierOption?.name ||
              "Conversão legada pendente"}
          </small>
          {Object.keys(item.attributesSnapshot || {}).length > 0 && (
            <small>{Object.values(item.attributesSnapshot).join(" / ")}</small>
          )}
          <small>
            {item.warehouse.name} · {quantity(item.quantity)} comercial /{" "}
            {quantity(item.baseQuantity)} {item.unitSnapshot || item.product.unit} BASE
          </small>
          <div className="order-entry-line-price">
            <span>{money(item.unitCost)} por apresentação</span>
          </div>
          <div className="row-actions">
            {has("purchases.update") && (
              <button
                type="button"
                className="secondary customer-icon-label"
                disabled={busy}
                onClick={() => onEdit(item)}
              >
                <Pencil size={16} /> Editar
              </button>
            )}
            {has("purchases.delete") && (
              <button
                type="button"
                className="danger-link customer-icon-label"
                disabled={busy}
                onClick={() => onRemove(item)}
              >
                <Trash2 size={16} /> Remover
              </button>
            )}
          </div>
        </article>
      ))}
      <div className="order-entry-total">
        <span>Total da compra</span>
        <strong>{purchase ? money(purchase.totalAmount) : "—"}</strong>
      </div>
      {children}
      {purchase && (
        <details className="order-entry-metadata">
          <summary>Informações do registro</summary>
          <RecordMetadata record={purchase} />
        </details>
      )}
    </section>
  );
}
