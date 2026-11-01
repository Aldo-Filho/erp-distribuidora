import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check, Package, RefreshCw } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { removeDraft, useDraft } from "../../auth/drafts";
import useConfirm from "../../components/feedback/useConfirm";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { orders } from "../../services/operations";
import { products } from "../../services/products";
import { productStock } from "../../services/productStock";
import useOrderEntry from "./useOrderEntry";
import OrderEntryHeader from "./OrderEntryHeader";
import OrderProductCatalog from "./OrderProductCatalog";
import OrderCart from "./OrderCart";
import TradeItemForm from "./TradeItemForm";
import DocumentStock from "./DocumentStock";
import { salesError } from "./salesErrors";
import "./orders.css";
import "./orderEntry.css";

export default function OrderEntryPage() {
  const { id = "new" } = useParams();
  const { session } = useAuth();
  if (id !== "new" && (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))))
    return <p>Pedido inválido.</p>;
  return <OrderEntry key={`${session.userId}:${id}`} id={id === "new" ? null : id} />;
}

function OrderEntry({ id }) {
  const { has, session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const confirm = useConfirm();
  const entry = useOrderEntry(id);
  const [savedOrigin, setSavedOrigin] = useDraft(
    `order-entry-origin:${session.userId}:${id || "new"}`,
    location.state?.orderList || null,
  );
  const originWriter = useRef(setSavedOrigin);
  originWriter.current = setSavedOrigin;
  useEffect(() => {
    if (location.state?.orderList) originWriter.current(location.state.orderList);
  }, [location.state]);
  const [selection, setSelection] = useDraft(
    `order-entry-selection:${session.userId}:${id || "new"}`,
    null,
  );
  const [warehouseId, setWarehouseId] = useDraft(
    `order-entry-warehouse:${session.userId}:${id || "new"}`,
    "",
  );
  const [creationAttempt, setCreationAttempt] = useDraft(
    `order-entry-creation:${session.userId}`,
    null,
  );
  const [editorState, setEditorState] = useState({ saving: false, dirty: false });
  const [headerState, setHeaderState] = useState({ saving: false, dirty: false, active: false });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [stockOpen, setStockOpen] = useState(false);
  const [quickKey, setQuickKey] = useState(null);
  const [creationCandidates, setCreationCandidates] = useState([]);
  const [checkingCreation, setCheckingCreation] = useState(false);
  const [stickyCart, setStickyCart] = useState(false);
  const cartSide = useRef(null);
  const editorClose = useRef(null);
  const headerClose = useRef(null);
  const nextSelection = useRef(null);
  const creating = useRef(false);
  const actionLock = useRef(false);
  const focusSearch = useRef(false);
  const pendingLeave = useRef(null);
  const selecting = useRef(false);
  const order = entry.order;
  const editable = order?.status === "PENDING";
  const busy = entry.writePending || editorState.saving || headerState.saving;
  const blocked = busy || entry.uncertain;
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
  }, [editable]);
  const reportEditor = useCallback((saving, dirty) => setEditorState({ saving, dirty }), []);
  const reportHeader = useCallback(
    (saving, dirty, active = true) => setHeaderState({ saving, dirty, active }),
    [],
  );
  useEffect(() => {
    const beforeUnload = (event) => {
      if (editorState.dirty || headerState.dirty || busy) {
        event.preventDefault();
        event.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, [editorState.dirty, headerState.dirty, busy]);
  useEffect(() => {
    if (!selection && focusSearch.current) {
      focusSearch.current = false;
      document
        .querySelector('#order-entry-products input[type="search"]')
        ?.focus({ preventScroll: true });
    }
  }, [selection]);
  const back = (viewOrderId) =>
    navigate("/orders", {
      state: { orderList: location.state?.orderList || savedOrigin, viewOrderId },
    });
  const leave = async (view = false) => {
    if (busy) return;
    if (selection && editorClose.current) {
      const dirty = editorState.dirty;
      if (
        dirty &&
        !(await confirm(
          "Descartar os campos do item ainda não salvos? Os itens já registrados serão mantidos.",
          { title: "Descartar alterações", confirmLabel: "Descartar", destructive: true },
        ))
      )
        return;
      removeDraft(
        `products:${session.userId}:school:order:${id}:item:${selection.item?.id || `entry:${selection.key || "new"}`}`,
      );
      setSelection(null);
    }
    if (headerState.active && headerClose.current) {
      pendingLeave.current = { view };
      await headerClose.current();
      pendingLeave.current = null;
      return;
    }
    back(view ? order?.id : undefined);
  };
  const select = async (product, item = null, quantity = 1) => {
    if (blocked || selecting.current || headerState.active || !editable) return;
    selecting.current = true;
    let target = { product, item, warehouseId, quantity, key: crypto.randomUUID() };
    try {
      if (!item) {
        const [presentations, stocks] = await Promise.all([
          products.presentations(product.id, { active: true }, 0, 25),
          has("stock.read") && has("warehouses.read") ? productStock.search(product.id) : null,
        ]);
        const presentation =
          presentations.content.find((row) => row.basePresentation && row.active) ||
          (presentations.totalElements === 1 ? presentations.content[0] : null);
        let depot = warehouseId;
        if (stocks && !stocks.some((row) => String(row.warehouse.id) === depot)) {
          depot = stocks.length === 1 ? String(stocks[0].warehouse.id) : "";
        }
        target = { ...target, presentation, warehouseId: depot };
      }
      if (selection && editorClose.current) {
        nextSelection.current = target;
        await editorClose.current();
        nextSelection.current = null;
      } else {
        setQuickKey(target.key);
        setSelection(target);
      }
      setMessage("");
    } catch (issue) {
      setError(salesError(issue.message));
    } finally {
      selecting.current = false;
    }
  };
  const closeEditor = () => {
    focusSearch.current = !nextSelection.current;
    setQuickKey(nextSelection.current?.key || null);
    setSelection(nextSelection.current);
    setEditorState({ saving: false, dirty: false });
  };
  const createOrSave = async (existing, body) => {
    if (existing) return entry.saveHeader(body);
    if (!has("orders.create") || creating.current || creationAttempt)
      throw new Error("Confira a listagem de vendas antes de tentar criar outro pedido.");
    creating.current = true;
    try {
      const filter = {
        customerId: body.customerId,
        userId: body.userId,
        paymentMethod: body.paymentMethod,
        size: 1,
      };
      const first = await orders.search(filter);
      const last =
        first.totalPages > 1
          ? await orders.search({ ...filter, page: first.totalPages - 1 })
          : first;
      setCreationAttempt({ ...body, afterId: last.content[0]?.id || 0 });
      const created = await orders.register(body);
      setCreationAttempt(null);
      return created;
    } catch (issue) {
      if ([400, 401, 403, 404, 409, 422].includes(issue.status)) setCreationAttempt(null);
      throw new Error(
        salesError(issue.message) +
          (issue.status
            ? ""
            : " Confira a listagem antes de iniciar outra venda: o pedido pode ter sido criado."),
      );
    } finally {
      creating.current = false;
    }
  };
  const writeItem = async (item, body) => {
    if (item) return entry.updateItem(item.id, body);
    return entry.addItem(body);
  };
  const runAction = async (work) => {
    if (actionLock.current || blocked) return;
    actionLock.current = true;
    setError("");
    try {
      await work();
    } catch (issue) {
      setError(salesError(issue.message));
    } finally {
      actionLock.current = false;
    }
  };
  const remove = (item) =>
    runAction(async () => {
      if (!has("orders.delete") || !editable) return;
      if (
        !(await confirm(
          `Remover ${item.productNameSnapshot || item.product.name} do pedido #${id}?`,
          { title: "Remover item", confirmLabel: "Remover", destructive: true },
        ))
      )
        return;
      await entry.removeItem(item.id);
      setMessage("Produto removido do pedido.");
    });
  const transition = (cancel = false) =>
    runAction(async () => {
      if (
        !has("orders.update") ||
        !has(cancel ? "orders.cancel" : "orders.confirm") ||
        selection ||
        headerState.active
      )
        return;
      if (
        !(await confirm(
          cancel
            ? `Cancelar o pedido #${id}? As reservas existentes serão liberadas.`
            : `Confirmar o pedido #${id} e reservar os itens no estoque?`,
          {
            title: cancel ? "Cancelar pedido" : "Confirmar venda",
            confirmLabel: cancel ? "Cancelar pedido" : "Confirmar e reservar",
            destructive: cancel,
          },
        ))
      )
        return;
      if (cancel) await entry.cancelOrder();
      else await entry.confirmOrder();
      setMessage(
        cancel ? "Pedido cancelado." : "Pedido confirmado. Os itens foram reservados no estoque.",
      );
    });
  if (!id && !has("orders.create")) return <p>Seu cargo não permite criar vendas.</p>;
  return (
    <div className="order-entry-page order-dialog">
      <header className="page-title">
        <div>
          <h1>{id ? `Registrar venda #${id}` : "Registrar venda"}</h1>
          <p className="field-hint">
            {order
              ? editable
                ? "Pendente · sem reserva de estoque"
                : "Pedido fora de edição"
              : "Selecione cliente e pagamento para iniciar."}
          </p>
        </div>
        <button
          type="button"
          className="secondary customer-icon-label"
          disabled={busy}
          onClick={() => leave()}
        >
          <ArrowLeft size={17} /> Vendas
        </button>
      </header>
      <ErrorAlert
        message={error || entry.error}
        retry={() => entry.reload().catch((issue) => setError(salesError(issue.message)))}
      />
      {message && (
        <p role="status" className="success">
          {message}
        </p>
      )}
      {entry.uncertain && !busy && (
        <div className="product-warning">
          <p>Há uma operação com resultado não confirmado. Novas gravações estão bloqueadas.</p>
          <button
            type="button"
            className="secondary"
            disabled={busy}
            onClick={() => runReconciliation()}
          >
            Conferir resultado
          </button>
        </div>
      )}
      {!id && creationAttempt && !busy && (
        <div role="alert" className="product-warning">
          <p>
            Confira os pedidos criados antes de iniciar outra venda. A tentativa anterior pode ter
            sido registrada.
          </p>
          <button
            type="button"
            className="secondary"
            disabled={checkingCreation || busy}
            onClick={checkCreation}
          >
            {checkingCreation ? "Consultando…" : "Conferir criação"}
          </button>
          {creationCandidates.map((candidate) => (
            <button
              type="button"
              className="secondary"
              key={candidate.id}
              onClick={() => {
                setCreationAttempt(null);
                navigate(`/orders/${candidate.id}/edit`, { replace: true, state: location.state });
              }}
            >
              Retomar pedido #{candidate.id}
            </button>
          ))}
        </div>
      )}
      {id && entry.loading ? (
        <p role="status">Carregando pedido…</p>
      ) : id && !order ? (
        <p>{entry.error ? "Não foi possível carregar o pedido." : "Pedido não encontrado."}</p>
      ) : (
        <>
          {(!id || editable) && (
            <section className="panel order-entry-heading-panel">
              <OrderEntryHeader
                order={order}
                defaultWarehouseId={warehouseId}
                onWarehouseChange={setWarehouseId}
                disabled={blocked || Boolean(selection)}
                canEdit={has("orders.update") && !selection}
                writeHeader={createOrSave}
                onStateChange={reportHeader}
                closeRef={headerClose}
                onClosed={() => {
                  if (pendingLeave.current) back(pendingLeave.current.view ? order?.id : undefined);
                }}
                onCreated={(created) => {
                  setWarehouseId("");
                  navigate(`/orders/${created.id}/edit`, { replace: true, state: location.state });
                }}
                onSaved={() => {
                  if (!id) back();
                  else
                    setMessage(
                      "Cliente e pagamento atualizados. Os preços dos itens existentes foram mantidos.",
                    );
                }}
              />
            </section>
          )}
          {order && !editable ? (
            <section className="panel">
              <p>Este pedido já não está pendente. A montagem foi encerrada.</p>
              <button type="button" className="secondary" onClick={() => back(order.id)}>
                Consultar pedido, despacho e histórico
              </button>
            </section>
          ) : (
            <>
              <nav className="order-entry-jumps" aria-label="Seções da venda">
                <a href="#order-entry-products">Adicionar produtos</a>
                <a href="#order-entry-cart">Ver pedido</a>
              </nav>
              <div className="order-entry-viewport">
                <div className="order-entry-workspace">
                  <OrderProductCatalog
                    order={order}
                    disabled={blocked || headerState.active}
                    onSelect={select}
                    selectedCatalogId={selection?.product?.catalogId}
                    editor={
                      selection &&
                      order && (
                        <TradeItemForm
                          key={selection.item?.id || `new:${selection.product.id}:${selection.key}`}
                          document={order}
                          item={selection.item}
                          existingItems={entry.items}
                          embedded
                          quickAdd={!selection.item && quickKey === selection.key}
                          initialSelection={selection}
                          writeItem={writeItem}
                          onDuplicate={(item) => select(item.product, item)}
                          closeRef={editorClose}
                          onStateChange={reportEditor}
                          onClose={closeEditor}
                          onSaved={(saved) => {
                            if (!warehouseId && saved?.warehouse?.id)
                              setWarehouseId(String(saved.warehouse.id));
                            focusSearch.current = true;
                            setSelection(null);
                            setMessage("Produto salvo no pedido.");
                          }}
                        />
                      )
                    }
                  />
                  <div
                    ref={cartSide}
                    className={"order-entry-side" + (stickyCart ? " is-sticky" : "")}
                  >
                    <OrderCart
                      order={order}
                      items={entry.items}
                      disabled={blocked || Boolean(selection) || headerState.active}
                      onEdit={(item) => select(item.product, item)}
                      onRemove={remove}
                    >
                      <div className="order-entry-cart-actions">
                        <button
                          type="button"
                          className="secondary"
                          disabled={blocked}
                          onClick={() => leave()}
                        >
                          Manter pendente
                        </button>
                        {order && (
                          <button
                            type="button"
                            className="secondary"
                            disabled={busy}
                            onClick={() => leave(true)}
                          >
                            Concluir edição e consultar
                          </button>
                        )}
                        {has("orders.update") && has("orders.confirm") && (
                          <button
                            type="button"
                            className="customer-icon-label"
                            disabled={
                              blocked ||
                              !order ||
                              !entry.items.length ||
                              Boolean(selection) ||
                              headerState.active
                            }
                            onClick={() => transition()}
                          >
                            <Check size={17} /> Confirmar e reservar
                          </button>
                        )}
                        {order && has("orders.update") && has("orders.cancel") && (
                          <button
                            type="button"
                            className="order-cancel-action"
                            disabled={blocked || Boolean(selection) || headerState.active}
                            onClick={() => transition(true)}
                          >
                            Cancelar pedido
                          </button>
                        )}
                      </div>
                      {order && (
                        <div className="order-entry-cart-tools">
                          <button
                            type="button"
                            className="secondary customer-icon-label"
                            disabled={busy}
                            onClick={() =>
                              entry.reload().catch((issue) => setError(salesError(issue.message)))
                            }
                          >
                            <RefreshCw size={16} /> Atualizar pedido
                          </button>
                          {has("stock.read") && entry.items.length > 0 && (
                            <button
                              type="button"
                              className="secondary customer-icon-label"
                              disabled={blocked}
                              onClick={() => setStockOpen(true)}
                            >
                              <Package size={16} /> Saldos e reservas
                            </button>
                          )}
                        </div>
                      )}
                    </OrderCart>
                  </div>
                </div>
              </div>
            </>
          )}
        </>
      )}
      {stockOpen && has("stock.read") && (
        <DocumentStock items={entry.items} onClose={() => setStockOpen(false)} />
      )}
    </div>
  );

  async function runReconciliation() {
    setError("");
    try {
      if (await entry.reconcile())
        setMessage("Resultado conferido. Revise os itens antes de continuar.");
    } catch (issue) {
      setError(salesError(issue.message));
    }
  }
  async function checkCreation() {
    if (!creationAttempt || checkingCreation) return;
    setCheckingCreation(true);
    setError("");
    try {
      const filter = {
        customerId: creationAttempt.customerId,
        userId: creationAttempt.userId,
        paymentMethod: creationAttempt.paymentMethod,
        size: 100,
      };
      const first = await orders.search(filter);
      const last =
        first.totalPages > 1
          ? await orders.search({ ...filter, page: first.totalPages - 1 })
          : first;
      const candidates = last.content.filter((candidate) => candidate.id > creationAttempt.afterId);
      setCreationCandidates(candidates);
      if (!candidates.length)
        setError(
          "A criação ainda não foi identificada. Consulte novamente ou confira a listagem de vendas antes de repetir.",
        );
    } catch (issue) {
      setError(salesError(issue.message));
    } finally {
      setCheckingCreation(false);
    }
  }
}
