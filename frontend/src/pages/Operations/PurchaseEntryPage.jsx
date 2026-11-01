import { ArrowLeft, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { removeDraft, useDraft } from "../../auth/drafts";
import useAuth from "../../auth/useAuth";
import useConfirm from "../../components/feedback/useConfirm";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import usePurchaseEntry from "./usePurchaseEntry";
import PurchaseEntryHeader from "./PurchaseEntryHeader";
import PurchaseProductCatalog from "./PurchaseProductCatalog";
import PurchaseCart from "./PurchaseCart";
import TradeItemForm from "./TradeItemForm";
import "./orderEntry.css";
import "./purchaseEntry.css";

export default function PurchaseEntryPage() {
  const { id } = useParams();
  const { has } = useAuth();
  if (id && !validId(id)) return <p role="alert">Identificador inválido.</p>;
  if (id ? !has("purchases.update") && !has("purchases.create") : !has("purchases.create"))
    return <p>Sem permissão para esta operação.</p>;
  return <Entry key={id || "new"} id={id ? Number(id) : null} />;
}
function Entry({ id }) {
  const { has, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const confirm = useConfirm();
  const { returnToList } = useListReturn({ modulePath: "/purchases" });
  const entry = usePurchaseEntry(id);
  const [selection, setSelection] = useDraft(`purchase-selection:${session.userId}:${id}`, null);
  const [headerDirty, setHeaderDirty] = useState(false);
  const [headerActive, setHeaderActive] = useState(false);
  const reportHeader = useCallback((saving, dirty, active = false) => {
    setHeaderDirty(saving || dirty);
    setHeaderActive(active);
  }, []);
  const headerClose = useRef(null);
  const leaving = useRef(false);
  const [error, setError] = useState("");
  const editorClose = useRef(null);
  const next = useRef(null);
  const removing = useRef(false);
  const selecting = useRef(false);
  const cartSide = useRef(null);
  const [stickyCart, setStickyCart] = useState(false);
  const purchase = entry.data?.purchase;
  useEffect(() => {
    const panel = cartSide.current;
    if (!panel) return;
    const update = () =>
      setStickyCart(panel.getBoundingClientRect().height <= window.innerHeight - 40);
    const observer = new ResizeObserver(update);
    observer.observe(panel);
    window.addEventListener("resize", update);
    update();
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", update);
    };
  }, [Boolean(purchase), purchase?.status]);
  const blocked = entry.busy || entry.uncertain || entry.refreshing || Boolean(entry.error);
  const select = async (value) => {
    if (blocked || headerActive || headerDirty || !purchase || selecting.current) return;
    selecting.current = true;
    try {
      if (!value.item) value = { ...value, key: crypto.randomUUID() };
      if (selection && editorClose.current) {
        next.current = { ...value, supplierId: purchase.supplier.id };
        await editorClose.current();
        next.current = null;
      } else setSelection({ ...value, supplierId: purchase.supplier.id });
    } finally {
      selecting.current = false;
    }
  };
  const remove = async (item) => {
    if (blocked || removing.current || !has("purchases.delete")) return;
    removing.current = true;
    try {
      if (
        !(await confirm("Remover este item da compra em rascunho?", {
          title: "Remover item",
          confirmLabel: "Remover",
          destructive: true,
        }))
      )
        return;
      await entry.removeItem(item.id);
    } catch (issue) {
      setError(issue.message);
    } finally {
      removing.current = false;
    }
  };
  const leave = async () => {
    if (entry.busy) return;
    leaving.current = true;
    try {
      if (selection && editorClose.current) {
        next.current = null;
        await editorClose.current();
      } else if (headerClose.current) await headerClose.current();
      else returnToList();
    } finally {
      leaving.current = false;
    }
  };
  const editor =
    selection && purchase && selection.supplierId === purchase.supplier.id ? (
      <TradeItemForm
        key={selection.item?.id || selection.key}
        purchase
        embedded
        document={purchase}
        item={selection.item}
        initialSelection={selection}
        closeRef={editorClose}
        onClose={() => {
          setSelection(next.current);
          if (leaving.current) returnToList();
        }}
        writeItem={(item, body) => (item ? entry.updateItem(item.id, body) : entry.addItem(body))}
        onSaved={() => setSelection(null)}
      />
    ) : null;
  return (
    <div className="operation-page order-entry-page purchases-page">
      <header className="page-title">
        <div>
          <h1>{id ? `Registrar compra #${id}` : "Registrar compra"}</h1>
          <p className="field-hint">
            {purchase
              ? purchase.status === "DRAFT"
                ? "Rascunho · sem entrada de estoque"
                : "Compra fora de edição"
              : "Selecione o fornecedor para iniciar."}
          </p>
        </div>
        <button
          type="button"
          className="secondary customer-icon-label"
          disabled={entry.busy}
          onClick={leave}
        >
          <ArrowLeft size={17} /> Compras
        </button>
      </header>
      <ErrorAlert message={error || entry.error} retry={entry.reload} />
      {entry.uncertain && (
        <section className="panel" role="alert">
          <p>
            A gravação pode ter sido registrada. Novos envios estão bloqueados. Consulte a compra ou
            a listagem antes de repetir; valores iguais não identificam uma inclusão.
          </p>
          <button className="secondary" onClick={entry.reload}>
            Consultar estado atual
          </button>
          <button
            className="secondary"
            onClick={async () => {
              if (
                await confirm(
                  "Você conferiu o estado atual e deseja liberar novas gravações? Nenhuma operação será repetida automaticamente.",
                  { title: "Conferir resultado", confirmLabel: "Conferi o resultado" },
                )
              )
                entry.acknowledge();
            }}
          >
            Conferi o resultado
          </button>
        </section>
      )}
      {id && entry.loading ? (
        <p role="status">Carregando compra…</p>
      ) : id && !purchase ? (
        <p>{entry.error ? "Compra indisponível." : "Compra não encontrada."}</p>
      ) : purchase && purchase.status !== "DRAFT" ? (
        <section className="panel">
          <p>
            A compra não está mais em rascunho. Os campos locais foram mantidos na memória desta
            aba.
          </p>
          <button onClick={() => navigate(`/purchases/${id}`, { state: location.state })}>
            Consultar compra
          </button>
        </section>
      ) : (
        <>
          <section className="panel order-entry-heading-panel">
            <PurchaseEntryHeader
              key={`${purchase?.supplier.id || "new"}:${purchase?.expectedDeliveryDate || ""}`}
              embedded
              purchase={purchase}
              disabled={blocked || Boolean(selection)}
              closeRef={headerClose}
              onStateChange={reportHeader}
              onClose={returnToList}
              onClosed={() => {
                if (leaving.current) returnToList();
              }}
              writeHeader={(_, body) => entry.saveHeader(body)}
              onSaved={(saved) => {
                if (!id)
                  navigate(`/purchases/${saved.id}/edit`, {
                    replace: true,
                    state: location.state,
                  });
              }}
            />
          </section>
          <nav className="order-entry-jumps" aria-label="Seções da compra">
            <a href="#purchase-catalog">Adicionar produtos</a>
            <a href="#purchase-cart">Ver compra</a>
          </nav>
          {selection && selection.supplierId !== purchase?.supplier.id && (
            <section className="panel" role="alert">
              <p>
                O fornecedor mudou. A seleção anterior foi mantida na memória, mas não pode ser
                enviada para este fornecedor.
              </p>
              <button
                className="secondary"
                onClick={async () => {
                  if (
                    await confirm("Descartar os campos da seleção anterior?", {
                      title: "Descartar seleção",
                      confirmLabel: "Descartar",
                      destructive: true,
                    })
                  ) {
                    removeDraft(
                      `products:${session.userId}:school:purchase:${id}:item:${selection.item?.id || `entry:${selection.key}`}`,
                    );
                    setSelection(null);
                  }
                }}
              >
                Descartar seleção anterior
              </button>
            </section>
          )}
          <div className="order-entry-viewport">
            <div className="order-entry-workspace">
              <PurchaseProductCatalog
                supplierId={purchase?.supplier.id}
                disabled={blocked || headerActive || !purchase || !has("purchases.create")}
                onSelect={select}
                editor={editor}
                selectedCatalogId={selection?.product.catalogId}
              />
              <div ref={cartSide} className={"order-entry-side" + (stickyCart ? " is-sticky" : "")}>
                <PurchaseCart
                  purchase={purchase}
                  items={entry.data?.items || []}
                  busy={blocked || headerActive || Boolean(selection)}
                  onEdit={(item) => select({ product: item.product, item, key: `item:${item.id}` })}
                  onRemove={remove}
                >
                  <div className="order-entry-cart-actions">
                    <button
                      type="button"
                      className="secondary"
                      disabled={entry.busy}
                      onClick={leave}
                    >
                      {purchase ? "Manter rascunho" : "Voltar às compras"}
                    </button>
                    {purchase && (
                      <button
                        type="button"
                        disabled={blocked || headerActive || Boolean(selection)}
                        onClick={() => navigate(`/purchases/${id}`, { state: location.state })}
                      >
                        Acompanhar compra
                      </button>
                    )}
                  </div>
                  {purchase && (
                    <div className="order-entry-cart-tools">
                      <button
                        type="button"
                        className="secondary customer-icon-label"
                        disabled={blocked || headerActive || Boolean(selection)}
                        onClick={entry.reload}
                      >
                        <RefreshCw size={16} /> Atualizar compra
                      </button>
                    </div>
                  )}
                </PurchaseCart>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
