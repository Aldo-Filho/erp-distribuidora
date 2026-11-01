import { Pencil, Trash2 } from "lucide-react";
import useAuth from "../../auth/useAuth";
import RecordMetadata from "../../components/record/RecordMetadata";
import { money, quantity } from "../Products/productFormData";

function subtotal(item) {
  const scaled = (value, places) => {
    const [whole, fraction = ""] = String(value ?? 0).split(".");
    return BigInt(whole + fraction.padEnd(places, "0"));
  };
  const gross = (scaled(item.unitPrice, 2) * scaled(item.quantity, 1) + 5n) / 10n;
  const discount =
    item.discountType === "PERCENTAGE"
      ? (gross * scaled(item.discount, 2) + 5000n) / 10000n
      : scaled(item.discount, 2);
  return money(Number(gross - discount) / 100);
}

export default function OrderCart({ order, items, disabled, onEdit, onRemove, children }) {
  const { has } = useAuth();
  return (
    <section
      className="panel order-entry-cart"
      id="order-entry-cart"
      aria-label="Produtos da venda"
    >
      <div className="order-entry-section-title">
        <h2>Produtos da venda</h2>
        <span className="field-hint">
          {items.length} {items.length === 1 ? "item" : "itens"}
        </span>
      </div>
      {!items.length && (
        <p className="empty">Escolha os produtos no catálogo para montar o pedido.</p>
      )}
      {items.map((item) => (
        <article key={item.id} className="order-entry-cart-line">
          <strong>{item.productNameSnapshot || item.product.name}</strong>
          <small>
            {item.skuSnapshot || item.product.sku} ·{" "}
            {item.presentationNameSnapshot || "Apresentação legada"}
          </small>
          {Object.keys(item.attributesSnapshot || {}).length > 0 && (
            <small>{Object.values(item.attributesSnapshot).join(" / ")}</small>
          )}
          <small>
            {item.warehouse.name} · {quantity(item.quantity)} comercial /{" "}
            {quantity(item.baseQuantity)} {item.unitSnapshot || item.product.unit}
          </small>
          <div className="order-entry-line-price">
            <span>{money(item.unitPrice)} por apresentação</span>
            <span>
              Desconto:{" "}
              {item.discountType === "PERCENTAGE"
                ? `${quantity(item.discount)}%`
                : money(item.discount)}
            </span>
          </div>
          <p className="field-hint">
            Subtotal (prévia): <strong>{subtotal(item)}</strong>
          </p>
          <div className="row-actions">
            {has("orders.update") && (
              <button
                className="secondary customer-icon-label"
                type="button"
                disabled={disabled || order?.status !== "PENDING"}
                onClick={() => onEdit(item)}
              >
                <Pencil size={16} /> Editar
              </button>
            )}
            {has("orders.delete") && (
              <button
                className="danger-link customer-icon-label"
                type="button"
                disabled={disabled || order?.status !== "PENDING"}
                onClick={() => onRemove(item)}
              >
                <Trash2 size={16} /> Remover
              </button>
            )}
          </div>
        </article>
      ))}
      <div className="order-entry-total">
        <span>Total do pedido</span>
        <strong>{money(order?.totalAmount ?? 0)}</strong>
      </div>
      {children}
      {order && (
        <details className="order-entry-metadata">
          <summary>Informações do registro</summary>
          <RecordMetadata record={order} />
        </details>
      )}
    </section>
  );
}
