import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronDown, X } from "lucide-react";

const formatQuantity = (value) => new Intl.NumberFormat("pt-BR").format(value);

export function ProductStockInfo({ items, unavailable }) {
  const [position, setPosition] = useState(null);
  const triggerRef = useRef(null);
  const panelRef = useRef(null);
  const panelId = useId();
  const isOpen = position !== null && !unavailable && items.length > 0;

  useEffect(() => {
    if (!isOpen) return;
    panelRef.current?.focus({ preventScroll: true });

    function closeOutside(event) {
      if (
        !triggerRef.current?.contains(event.target) &&
        !panelRef.current?.contains(event.target)
      ) {
        setPosition(null);
      }
    }
    function closeOnEscape(event) {
      if (event.key === "Escape") {
        event.preventDefault();
        setPosition(null);
        triggerRef.current?.focus({ preventScroll: true });
      }
    }
    function closeOnScroll(event) {
      if (!panelRef.current?.contains(event.target)) setPosition(null);
    }
    function closeOnResize() {
      setPosition(null);
    }

    document.addEventListener("pointerdown", closeOutside);
    document.addEventListener("focusin", closeOutside);
    document.addEventListener("keydown", closeOnEscape);
    window.addEventListener("scroll", closeOnScroll, true);
    window.addEventListener("resize", closeOnResize);
    return () => {
      document.removeEventListener("pointerdown", closeOutside);
      document.removeEventListener("focusin", closeOutside);
      document.removeEventListener("keydown", closeOnEscape);
      window.removeEventListener("scroll", closeOnScroll, true);
      window.removeEventListener("resize", closeOnResize);
    };
  }, [isOpen]);

  function togglePanel() {
    if (isOpen) {
      setPosition(null);
      return;
    }
    const rect = triggerRef.current.getBoundingClientRect();
    const margin = 12;
    const gap = 8;
    const width = Math.min(340, window.innerWidth - margin * 2);
    const spaceBelow = window.innerHeight - rect.bottom - gap - margin;
    const spaceAbove = rect.top - gap - margin;
    const openAbove = spaceBelow < 260 && spaceAbove > spaceBelow;
    setPosition({
      width,
      left: Math.max(margin, Math.min(rect.left, window.innerWidth - width - margin)),
      ...(openAbove ? { bottom: window.innerHeight - rect.top + gap } : { top: rect.bottom + gap }),
      maxHeight: Math.min(400, Math.max(0, openAbove ? spaceAbove : spaceBelow)),
    });
  }

  if (unavailable) return <span className="text-xs text-slate-500">Estoque indisponível</span>;
  if (items.length === 0)
    return <span className="text-xs text-slate-500">Sem estoque cadastrado</span>;

  const quantity = items.reduce((total, item) => total + (item.quantity ?? 0), 0);
  const reserved = items.reduce((total, item) => total + (item.reservedQuantity ?? 0), 0);

  return (
    <div className="flex min-w-0 items-center gap-2 py-2 pr-2">
      <button
        type="button"
        ref={triggerRef}
        onClick={togglePanel}
        className={`grid h-8 w-8 shrink-0 place-items-center rounded-md outline-none hover:bg-blue-50 hover:text-blue-700 focus-visible:ring-2 focus-visible:ring-blue-500 ${isOpen ? "bg-blue-50 text-blue-700" : "text-slate-500"}`}
        aria-label={`Ver estoque por armazém (${items.length})`}
        title="Ver estoque por armazém"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-controls={isOpen ? panelId : undefined}
      >
        <ChevronDown
          size={17}
          strokeWidth={1.8}
          aria-hidden="true"
          className={`transition-transform ${isOpen ? "rotate-180" : ""}`}
        />
      </button>
      <dl className="grid min-w-0 flex-1 grid-cols-3 gap-2 text-xs">
        <div>
          <dt className="text-slate-500">Estoque</dt>
          <dd className="mt-1 font-semibold">{formatQuantity(quantity)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Reservado</dt>
          <dd className="mt-1 font-semibold">{formatQuantity(reserved)}</dd>
        </div>
        <div>
          <dt className="text-slate-500">Disponível</dt>
          <dd className="mt-1 font-semibold text-blue-700">
            {formatQuantity(quantity - reserved)}
          </dd>
        </div>
      </dl>
      {isOpen &&
        createPortal(
          <section
            id={panelId}
            ref={panelRef}
            role="dialog"
            aria-label="Estoque por armazém"
            tabIndex={-1}
            style={position}
            className="fixed z-30 overflow-y-auto overscroll-contain rounded-xl border border-slate-200 bg-white p-4 text-xs text-slate-800 shadow-xl outline-none"
          >
            <header className="mb-3 flex items-center justify-between gap-3">
              <h3 className="text-sm font-semibold text-slate-900">Estoque por armazém</h3>
              <button
                type="button"
                className="grid h-8 w-8 shrink-0 place-items-center rounded-md text-slate-500 hover:bg-slate-100"
                aria-label="Fechar estoque por armazém"
                onClick={() => {
                  setPosition(null);
                  triggerRef.current?.focus({ preventScroll: true });
                }}
              >
                <X size={16} aria-hidden="true" />
              </button>
            </header>
            <ul className="grid gap-2">
              {items.map((item) => (
                <li key={item.id} className="rounded-lg bg-slate-50 p-2">
                  <strong className="break-words font-semibold">
                    {item.warehouse?.name || "Armazém não informado"}
                  </strong>
                  <dl className="mt-1 grid grid-cols-2 gap-x-2 gap-y-1 text-slate-600">
                    <div>
                      <dt className="inline">Estoque: </dt>
                      <dd className="inline">{formatQuantity(item.quantity ?? 0)}</dd>
                    </div>
                    <div>
                      <dt className="inline">Reservado: </dt>
                      <dd className="inline">{formatQuantity(item.reservedQuantity ?? 0)}</dd>
                    </div>
                    <div className="col-span-2">
                      <dt className="inline">Disponível: </dt>
                      <dd className="inline">
                        {formatQuantity((item.quantity ?? 0) - (item.reservedQuantity ?? 0))}
                      </dd>
                    </div>
                    <div>
                      <dt className="inline">Mínimo: </dt>
                      <dd className="inline">{formatQuantity(item.minQuantity ?? 0)}</dd>
                    </div>
                    <div>
                      <dt className="inline">Máximo: </dt>
                      <dd className="inline">
                        {item.maxQuantity == null
                          ? "Não definido"
                          : formatQuantity(item.maxQuantity)}
                      </dd>
                    </div>
                  </dl>
                </li>
              ))}
            </ul>
          </section>,
          document.body,
        )}
    </div>
  );
}
