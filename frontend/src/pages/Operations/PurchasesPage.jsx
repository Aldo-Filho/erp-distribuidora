import { useEffect, useState } from "react";
import { Eye, Pencil, Plus, Trash2 } from "lucide-react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import Pagination from "../../components/feedback/Pagination";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import FilterSelect from "../../components/feedback/FilterSelect";
import RowActions from "../../components/feedback/RowActions";
import RecordMetadata from "../../components/record/RecordMetadata";
import { purchases } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductForm from "../Products/useProductForm";
import useProductAction from "../Products/useProductAction";
import { money, quantity } from "../Products/productFormData";
import { formatRecordDateTime } from "../../utils/dateTime";
import { PartyPicker, partyName } from "./OperationSelectors";
import TradeItemForm from "./TradeItemForm";
import StockHistory from "./StockHistory";
import DocumentStock from "./DocumentStock";
import PurchaseReceiptForm, { pendingReceipt, pendingBase, clearPendingReceipt } from "./PurchaseReceiptForm";
import "../Products/products.css";
import "./operations.css";
import "./purchases.css";

const statuses = {
  DRAFT: "Rascunho",
  SENT: "Enviada",
  CONFIRMED: "Confirmada",
  PARTIALLY_RECEIVED: "Recebida parcialmente",
  RECEIVED: "Recebida",
  CANCELED: "Cancelada",
};
export default function PurchasesPage() {
  const { has } = useAuth();
  const [status, setStatusState] = useState("");
  const setStatus = (next) => { setPage(0); setStatusState(next); };
  const [page, setPage] = useState(0);
  const [flow, setFlow] = useState(null);
  const [notice, setNotice] = useState("");
  const state = useProductResource(
    async (options) => ({
      status,
      page,
      records: await purchases.search({ status, page, size: 20 }, options),
    }),
    [status, page],
  );
  const loading = Boolean(
    state.loading || (state.data && (state.data.status !== status || state.data.page !== page)),
  );
  useEffect(() => {
    if (loading || state.error || !state.data) return;
    const lastPage = Math.max(0, state.data.records.totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }, [loading, state.error, state.data, page]);
  const records = loading || state.error ? null : state.data?.records?.content;
  const filtered = Boolean(status);
  return (
    <div className="customers-page operation-documents purchases-page">
      <header className="page-title">
        <PageHeading
          title="Compras"
          total={state.data?.records?.totalElements}
          loading={loading}
          singular={filtered ? "compra encontrada" : "compra cadastrada"}
          plural={filtered ? "compras encontradas" : "compras cadastradas"}
        />
        {has("purchases.create") && (
          <button className="customer-icon-label" onClick={() => setFlow({ type: "new" })}>
            <Plus size={17} /> Nova compra
          </button>
        )}
      </header>
      <section className="panel customer-filters" aria-label="Filtros de compras">
        <label>
          Situação
          <FilterSelect value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todas</option>
            {Object.entries(statuses).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </FilterSelect>
        </label>
        <button type="button" className="secondary" onClick={() => setStatus("")}>
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={!loading && state.error} retry={state.reload} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <section className="panel table-panel" aria-label="Lista de compras" aria-busy={loading}>
        {loading ? (
          <p role="status">Carregando compras…</p>
        ) : state.error ? (
          <p>Não foi possível carregar as compras.</p>
        ) : !records?.length ? (
          <p className="empty">
            {filtered
              ? "Nenhuma compra encontrada para estes filtros."
              : "Nenhuma compra cadastrada."}
          </p>
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Compra</th>
                    <th>Fornecedor</th>
                    <th>Situação</th>
                    <th>Total</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((purchase) => (
                    <tr key={purchase.id}>
                      <td>
                        <strong>#{purchase.id}</strong>
                        <small>
                          Entrega prevista:{" "}
                          {purchase.expectedDeliveryDate
                            ? purchase.expectedDeliveryDate.split("-").reverse().join("/")
                            : "Não informada"}
                        </small>
                      </td>
                      <td>
                        <strong
                          className="purchase-supplier-name"
                          title={partyName(purchase.supplier)}
                        >
                          {partyName(purchase.supplier)}
                        </strong>
                      </td>
                      <td>
                        <PurchaseStatus status={purchase.status} />
                      </td>
                      <td>
                        <strong>{money(purchase.totalAmount)}</strong>
                      </td>
                      <td>
                        <RowActions
                          className="customer-row-actions"
                          label={`Ações da compra ${purchase.id}`}
                        >
                          <button
                            type="button"
                            className="secondary icon-button"
                            aria-label={`Detalhes da compra ${purchase.id}`}
                            title="Consultar compra"
                            onClick={() => setFlow({ type: "details", id: purchase.id })}
                          >
                            <Eye size={17} />
                          </button>
                        </RowActions>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="customer-list-footer">
              <span>
                {state.data.records.totalElements.toLocaleString("pt-BR")}{" "}
                {state.data.records.totalElements === 1 ? "registro" : "registros"}
              </span>
            </div>
          </>
        )}
      </section>
      <Pagination
        page={page}
        totalPages={state.data?.records?.totalPages || 0}
        change={setPage}
      />
      {flow?.type === "new" && (
        <PurchaseHeaderForm
          onClose={() => setFlow(null)}
          onSaved={(purchase) => {
            setNotice(`Compra #${purchase.id} registrada. Adicione os itens antes de enviar.`);
            state.reload();
            setFlow({ type: "details", id: purchase.id });
          }}
        />
      )}
      {flow?.type === "details" && (
        <PurchaseDetails id={flow.id} onClose={() => setFlow(null)} onChanged={state.reload} />
      )}
    </div>
  );
}
function PurchaseStatus({ status }) {
  return (
    <span className={`badge purchase-status purchase-status-${status?.toLowerCase()}`}>
      {statuses[status] || status}
    </span>
  );
}
function PurchaseHeaderForm({ purchase, onClose, onSaved }) {
  const { has, session } = useAuth();
  const [picker, setPicker] = useState(false);
  const form = useProductForm(
    `school:purchase:${purchase?.id || "new"}`,
    {
      supplier: purchase?.supplier || null,
      expectedDeliveryDate: purchase?.expectedDeliveryDate || "",
    },
    onClose,
    onSaved,
  );
  return (
    <Modal
      title={purchase ? "Editar compra em rascunho" : "Nova compra"}
      onClose={form.close}
      busy={form.saving}
      wide
      className="purchase-dialog"
    >
      <form
        className="customer-form"
        onSubmit={(e) =>
          form.submit(e, (value) => {
            if (!value.supplier) throw new Error("Selecione um fornecedor.");
            const body = { supplierId: value.supplier.id };
            if (value.expectedDeliveryDate) body.expectedDeliveryDate = value.expectedDeliveryDate;
            else if (purchase?.expectedDeliveryDate) body.removeExpectedDeliveryDate = true;
            return purchase
              ? purchases.update(purchase.id, body)
              : purchases.register({ ...body, userId: session.userId });
          })
        }
      >
        <ErrorAlert message={form.error} />
        <fieldset
          disabled={form.saving || !has(purchase ? "purchases.update" : "purchases.create")}
          className="customer-form-fields"
        >
          <div className="form-grid">
            <label className="span-all">
              Fornecedor
              <div className="purchase-supplier-control">
                <input
                  readOnly
                  value={form.value.supplier ? partyName(form.value.supplier) : ""}
                  placeholder="Selecione um fornecedor"
                />
                <button
                  type="button"
                  className="secondary"
                  disabled={!has("suppliers.read")}
                  onClick={() => setPicker(true)}
                >
                  {form.value.supplier ? "Trocar fornecedor" : "Selecionar fornecedor"}
                </button>
              </div>
            </label>
            <label>
              Entrega prevista (opcional)
              <input
                type="date"
                value={form.value.expectedDeliveryDate}
                onChange={(e) => form.change("expectedDeliveryDate", e.target.value)}
              />
            </label>
            <p className="field-hint span-all">
              Rascunho → enviada → confirmada → recebimento. Enviar registra o compromisso; não
              envia mensagem ao fornecedor.
            </p>
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            Cancelar
          </button>
          <button
            disabled={form.saving || !has(purchase ? "purchases.update" : "purchases.create")}
          >
            {form.saving ? "Salvando…" : "Salvar compra"}
          </button>
        </div>
      </form>
      {picker && (
        <PartyPicker
          purchase
          onClose={() => setPicker(false)}
          onSelect={(supplier) => {
            form.change("supplier", supplier);
            setPicker(false);
          }}
        />
      )}
    </Modal>
  );
}
function PurchaseDetails({ id, onClose, onChanged }) {
  const { has, session } = useAuth();
  const [flow, setFlow] = useState(null);
  const [revision, setRevision] = useState(0);
  const action = useProductAction();
  const state = useProductResource(
    async (options) => {
      const [purchase, items, receipts] = await Promise.all([
        purchases.findOne(id, options),
        purchases.items(id, options),
        purchases.receipts(id, options),
      ]);
      return purchase ? { purchase, items, receipts } : null;
    },
    [id, revision],
  );
  const refresh = () => {
    setRevision((n) => n + 1);
    state.reload();
    onChanged();
  };
  const changed = () => {
    setFlow(null);
    action.succeed();
    refresh();
  };
  const purchase = state.data?.purchase;
  const [reconciledReceipt, setReconciledReceipt] = useState(null);
  useEffect(() => {
    if (!state.data?.receipts || flow?.type === "receive") return;
    let attempt;
    try {
      attempt = pendingReceipt(session.userId, id);
    } catch {
      return; // The receipt form reports storage failures and blocks unsafe submissions.
    }
    if (!attempt) return;
    const registered = state.data.receipts.find(
      (receipt) => receipt.requestId === attempt.requestId,
    );
    if (registered) {
      try {
        clearPendingReceipt(session.userId, id);
      } catch {
        // The receipt remains confirmed in the API even if local cleanup fails.
      }
      setReconciledReceipt(registered.id);
    }
  }, [state.data, flow?.type, session.userId, id]);
  const canChangeStatus = has("purchases.update");
  const transition = (status, message, label) =>
    action.run(
      async () => {
        await purchases.update(id, { status });
        refresh();
      },
      message,
      { title: label, confirmLabel: label, destructive: status === "CANCELED" },
    );
  let pending = null;
  let receiptStorageError = false;
  try {
    pending = pendingReceipt(session.userId, id);
  } catch {
    receiptStorageError = true;
  }
  return (
    <Modal
      title={`Detalhes da compra #${id}`}
      onClose={onClose}
      busy={action.pending}
      wide
      className="purchase-dialog"
    >
      <ErrorAlert message={action.error} />
      {receiptStorageError && (
        <ErrorAlert message="Não foi possível acessar a tentativa de recebimento neste navegador. O formulário impedirá novos envios até que o armazenamento esteja disponível." />
      )}
      {reconciledReceipt && (
        <p className="success" role="status">
          A tentativa pendente foi identificada no histórico como recibo #{reconciledReceipt}.
          Nenhum novo recebimento foi enviado.
        </p>
      )}
      {action.notice && (
        <p className="success" role="status">
          {action.notice}
        </p>
      )}
      <ProductLoad state={state} empty="Compra não encontrada.">
        {purchase && (
          <>
            <div className="customer-detail-title">
              <h3>{partyName(purchase.supplier)}</h3>
              <PurchaseStatus status={purchase.status} />
            </div>
            <dl className="customer-details">
              <div>
                <dt>Compra</dt>
                <dd>#{purchase.id}</dd>
              </div>
              <div>
                <dt>Entrega prevista</dt>
                <dd>
                  {purchase.expectedDeliveryDate
                    ? purchase.expectedDeliveryDate.split("-").reverse().join("/")
                    : "Não informada"}
                </dd>
              </div>
              <div>
                <dt>Total</dt>
                <dd>
                  <strong>{money(purchase.totalAmount)}</strong>
                </dd>
              </div>
            </dl>
            <div className="operation-toolbar purchase-actions">
              <button className="secondary" disabled={action.pending} onClick={refresh}>
                Atualizar
              </button>
              {purchase.status === "DRAFT" && has("purchases.update") && (
                <button
                  className="secondary"
                  disabled={action.pending}
                  onClick={() => setFlow({ type: "header" })}
                >
                  Editar compra
                </button>
              )}
              {purchase.status === "DRAFT" && has("purchases.create") && (
                <button disabled={action.pending} onClick={() => setFlow({ type: "item" })}>
                  <Plus size={17} /> Adicionar item
                </button>
              )}
              {purchase.status === "DRAFT" && canChangeStatus && has("purchases.send") && (
                <button
                  disabled={action.pending}
                  onClick={() =>
                    transition(
                      "SENT",
                      "Registrar esta compra como enviada? Produto, embalagem e valores ficarão contratados.",
                      "Registrar envio",
                    )
                  }
                >
                  Registrar envio
                </button>
              )}
              {purchase.status === "SENT" && canChangeStatus && has("purchases.confirm") && (
                <button
                  disabled={action.pending}
                  onClick={() =>
                    transition(
                      "CONFIRMED",
                      "Confirmar esta compra para permitir o recebimento?",
                      "Confirmar compra",
                    )
                  }
                >
                  Confirmar compra
                </button>
              )}
              {["DRAFT", "SENT", "CONFIRMED"].includes(purchase.status) &&
                !pending &&
                canChangeStatus &&
                has("purchases.cancel") && (
                  <button
                    className="secondary"
                    disabled={action.pending}
                    onClick={() =>
                      transition(
                        "CANCELED",
                        "Cancelar esta compra ainda sem recebimento?",
                        "Cancelar compra",
                      )
                    }
                  >
                    Cancelar compra
                  </button>
                )}
              {(pending || receiptStorageError || ["CONFIRMED", "PARTIALLY_RECEIVED"].includes(purchase.status)) &&
                has("purchases.receive") && (
                  <button disabled={action.pending} onClick={() => setFlow({ type: "receive" })}>
                    {pending ? "Retomar recebimento" : "Receber em BASE"}
                  </button>
                )}
              {has("stock.read") && (
                <>
                  <button
                    className="secondary"
                    disabled={action.pending}
                    onClick={() => setFlow({ type: "stock" })}
                  >
                    Consultar saldos
                  </button>
                  <button
                    className="secondary"
                    disabled={action.pending}
                    onClick={() => setFlow({ type: "history" })}
                  >
                    Histórico de entradas
                  </button>
                </>
              )}
            </div>
            <div className="purchase-section-heading">
              <h3>Itens da compra</h3>
              <span>
                {state.data.items.length} {state.data.items.length === 1 ? "item" : "itens"}
              </span>
            </div>
            <section className="panel table-panel" aria-label="Itens da compra">
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Produto / opção</th>
                      <th>Depósito</th>
                      <th>Comercial / custo</th>
                      <th>BASE: contratado / recebido / pendente</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.data.items.map((item) => (
                      <tr key={item.id}>
                        <td>
                          <strong>{item.productNameSnapshot || item.product.name}</strong>
                          <small
                            title={`${item.skuSnapshot || item.product.sku} · ${item.presentationNameSnapshot || "Conversão legada pendente"}`}
                          >
                            {item.skuSnapshot || item.product.sku} ·{" "}
                            {item.presentationNameSnapshot || "Conversão legada pendente"}
                          </small>
                          {Object.keys(item.attributesSnapshot || {}).length > 0 && (
                            <small title={Object.values(item.attributesSnapshot).join(" / ")}>
                              {Object.values(item.attributesSnapshot).join(" / ")}
                            </small>
                          )}
                        </td>
                        <td>{item.warehouse.name}</td>
                        <td>
                          {quantity(item.quantity)} · {money(item.unitCost)}
                        </td>
                        <td>
                          {quantity(item.baseQuantity)} / {quantity(item.receivedQuantity)} /{" "}
                          {pendingBase(item) ?? "Pendente de conversão"}
                          <small>{item.unitSnapshot || item.product.unit}</small>
                        </td>
                        <td>
                          {purchase.status === "DRAFT" && (
                            <RowActions
                              className="customer-row-actions"
                              label={`Ações do item ${item.id}`}
                            >
                              {has("purchases.update") && (
                                <button
                                  type="button"
                                  className="secondary icon-button"
                                  aria-label={`Editar item ${item.id}`}
                                  title="Editar item"
                                  disabled={action.pending}
                                  onClick={() => setFlow({ type: "item", item })}
                                >
                                  <Pencil size={17} />
                                </button>
                              )}
                              {has("purchases.delete") && (
                                <button
                                  type="button"
                                  aria-label={`Remover item ${item.id}`}
                                  title="Remover item"
                                  className="secondary icon-button danger-link"
                                  disabled={action.pending}
                                  onClick={() =>
                                    action.run(
                                      async () => {
                                        await purchases.deleteItem(item.id);
                                        refresh();
                                      },
                                      "Remover este item da compra em rascunho?",
                                      {
                                        title: "Remover item",
                                        confirmLabel: "Remover",
                                        destructive: true,
                                      },
                                    )
                                  }
                                >
                                  <Trash2 size={17} />
                                </button>
                              )}
                            </RowActions>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {!state.data.items.length && (
                <p className="empty">Nenhum item. Adicione um produto para continuar.</p>
              )}
            </section>
            <div className="purchase-section-heading">
              <h3>Recebimentos registrados</h3>
              <span>
                {state.data.receipts.length}{" "}
                {state.data.receipts.length === 1 ? "recibo" : "recibos"}
              </span>
            </div>
            {!state.data.receipts.length && <p>Nenhum recebimento registrado.</p>}
            {state.data.receipts.map((receipt) => (
              <section className="purchase-receipt-card" key={receipt.id}>
                <strong>
                  Recibo #{receipt.id} · {formatRecordDateTime(receipt.receivedAt)}
                </strong>
                <dl className="customer-details">
                  <div>
                    <dt>Operador</dt>
                    <dd>
                      {receipt.operator?.name || receipt.operator?.username || ""} #
                      {receipt.operator?.id}
                    </dd>
                  </div>
                  <div>
                    <dt>Identificador do recebimento</dt>
                    <dd>{receipt.requestId}</dd>
                  </div>
                </dl>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Produto / item</th>
                        <th>Depósito</th>
                        <th>Quantidade BASE</th>
                      </tr>
                    </thead>
                    <tbody>
                      {receipt.items.map((line) => (
                        <tr key={line.id}>
                          <td>
                            <strong>
                              {line.purchaseItem.productNameSnapshot ||
                                line.purchaseItem.product.name}
                            </strong>
                            <small>
                              Item #{line.purchaseItem.id} ·{" "}
                              {line.purchaseItem.skuSnapshot || line.purchaseItem.product.sku}
                            </small>
                          </td>
                          <td>{line.stockItem.warehouse.name}</td>
                          <td>
                            {quantity(line.quantity)}{" "}
                            {line.purchaseItem.unitSnapshot || line.purchaseItem.product.unit}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {receipt.notes && <p className="field-hint">{receipt.notes}</p>}
              </section>
            ))}
            <RecordMetadata record={purchase} />
          </>
        )}
      </ProductLoad>
      <div className="actions">
        <button type="button" className="secondary" disabled={action.pending} onClick={onClose}>
          Fechar
        </button>
      </div>
      {flow?.type === "header" && (
        <PurchaseHeaderForm purchase={purchase} onClose={() => setFlow(null)} onSaved={changed} />
      )}
      {flow?.type === "item" && (
        <TradeItemForm
          purchase
          document={purchase}
          item={flow.item}
          onClose={() => setFlow(null)}
          onSaved={changed}
        />
      )}
      {flow?.type === "receive" && (
        <PurchaseReceiptForm
          purchase={purchase}
          items={state.data.items}
          onClose={() => setFlow(null)}
          onSaved={changed}
        />
      )}
      {flow?.type === "history" && (
        <StockHistory filter={{ purchaseId: id }} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "stock" && (
        <DocumentStock items={state.data.items} revision={revision} onClose={() => setFlow(null)} />
      )}
    </Modal>
  );
}
