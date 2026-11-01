import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import Modal from "../../components/feedback/Modal";
import { quantity } from "./productFormData";
import { available } from "../Operations/StockPage";

export default function ProductStockPopover({ item, stocks, anchor, id, onClose, onManage }) {
  const [position, setPosition] = useState(null);
  const [mobile] = useState(() => window.matchMedia("(max-width: 600px)").matches);
  const panel = useRef(null);
  const closeButton = useRef(null);
  const titleId = useId();

  useLayoutEffect(() => {
    if (mobile) return;
    const rect = anchor.current.getBoundingClientRect();
    const width = Math.min(380, window.innerWidth - 24);
    const below = window.innerHeight - rect.bottom - 18;
    const above = rect.top - 18;
    const opensAbove = below < 240 && above > below;
    setPosition({
      left: Math.max(12, Math.min(rect.left, window.innerWidth - width - 12)),
      ...(opensAbove ? { bottom: window.innerHeight - rect.top + 6 } : { top: rect.bottom + 6 }),
      width,
      maxHeight: Math.max(80, opensAbove ? above : below),
    });
  }, [anchor, mobile]);

  useEffect(() => {
    if (mobile || !position) return;
    closeButton.current?.focus({ preventScroll: true });
    const rect = anchor.current.getBoundingClientRect();
    const outside = (event) => {
      if (!panel.current?.contains(event.target) && !anchor.current?.contains(event.target)) {
        onClose(false);
      }
    };
    const scroll = (event) => {
      if (panel.current?.contains(event.target)) return;
      const current = anchor.current?.getBoundingClientRect();
      if (!current || Math.abs(current.top - rect.top) > 1 || Math.abs(current.left - rect.left) > 1) {
        onClose(false);
      }
    };
    const resize = () => onClose(false);
    document.addEventListener("pointerdown", outside);
    document.addEventListener("focusin", outside);
    window.addEventListener("scroll", scroll, true);
    window.addEventListener("resize", resize);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("focusin", outside);
      window.removeEventListener("scroll", scroll, true);
      window.removeEventListener("resize", resize);
    };
  }, [anchor, mobile, onClose, position]);

  const content = (
    <>
      <p className="field-hint">{item.sku} · Quantidades em {item.unit || "unidade pendente"}.</p>
      <div className="product-stock-preview-depots">
        {stocks.length ? stocks.map((stock) => (
          <section key={stock.id} className="product-stock-preview-depot">
            <h4>{stock.warehouse.name}</h4>
            <dl>
              <div><dt>Estoque</dt><dd>{quantity(stock.quantity)}</dd></div>
              <div className="product-stock-reserved">
                <dt>Reservado</dt><dd>{quantity(stock.reservedQuantity)}</dd>
              </div>
              <div className="product-stock-available">
                <dt>Disponível</dt><dd>{available(stock)}</dd>
              </div>
            </dl>
          </section>
        )) : <p className="empty">Nenhum estoque configurado para este produto.</p>}
      </div>
      <div className="actions">
        <button type="button" onClick={onManage}>Gerenciar estoque</button>
      </div>
    </>
  );

  if (mobile) {
    return createPortal(
      <Modal title="Estoque por depósito" onClose={() => onClose(true)}>
        <div id={id}>{content}</div>
      </Modal>,
      document.body,
    );
  }
  if (!position) return null;
  return createPortal(
    <section
      id={id}
      ref={panel}
      role="dialog"
      aria-modal="false"
      aria-labelledby={titleId}
      className="product-stock-popover"
      style={position}
      onKeyDown={(event) => {
        if (event.key === "Escape") {
          event.preventDefault();
          event.stopPropagation();
          onClose(true);
        }
      }}
    >
      <div className="product-stock-popover-heading">
        <h3 id={titleId}>Estoque por depósito</h3>
        <button
          ref={closeButton}
          type="button"
          className="secondary icon-button"
          aria-label="Fechar consulta de estoque"
          onClick={() => onClose(true)}
        >
          <X size={18} aria-hidden="true" />
        </button>
      </div>
      {content}
    </section>,
    anchor.current?.closest('[role="dialog"]') || document.body,
  );
}
