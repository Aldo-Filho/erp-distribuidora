import { useEffect, useRef, useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { removeDraft, saveDraft, useDraft } from "../../auth/drafts";
import { activePurchaseWrites } from "./usePurchaseEntry";
import useConfirm from "../../components/feedback/useConfirm";
import useAuth from "../../auth/useAuth";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RowActions from "../../components/feedback/RowActions";
import RecordMetadata from "../../components/record/RecordMetadata";
import { purchases } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductAction from "../Products/useProductAction";
import { money, quantity } from "../Products/productFormData";
import { formatRecordDateTime } from "../../utils/dateTime";
import { partyName } from "./OperationSelectors";
import StockHistory from "./StockHistory";
import DocumentStock from "./DocumentStock";
import PurchaseReceiptForm, {
  pendingReceipt,
  pendingBase,
  clearPendingReceipt,
} from "./PurchaseReceiptForm";
import "../Products/products.css";
import "./operations.css";
import "./purchases.css";

import EmbeddedContent from "../../components/layout/EmbeddedContent";

const statuses = {
  DRAFT: "Rascunho",
  SENT: "Enviada",
  CONFIRMED: "Confirmada",
  PARTIALLY_RECEIVED: "Recebida parcialmente",
  RECEIVED: "Recebida",
  CANCELED: "Cancelada",
};
function PurchaseStatus({ status }) {
  return (
    <span className={`badge purchase-status purchase-status-${status?.toLowerCase()}`}>
      {statuses[status] || status}
    </span>
  );
}
export default function PurchaseDetailsContent({
  id,
  onClose,
  onChanged = () => {},
  onEdit,
  embedded = false,
  leaveRef,
}) {
  const Container = EmbeddedContent;
  const { has, session } = useAuth();
  const [flow, setFlow] = useState(null);
  const receiptClose = useRef(null);
  const leave = () => (flow?.type === "receive" ? receiptClose.current?.() : onClose());
  useEffect(() => {
    if (leaveRef) leaveRef.current = leave;
    return () => {
      if (leaveRef) leaveRef.current = null;
    };
  });
  const [revision, setRevision] = useState(0);
  const action = useProductAction();
  const confirm = useConfirm();
  const [uncertain, setUncertain] = useDraft(`purchase-write:${session.userId}:${id}`, false);
  const attemptKey = `purchase-write:${session.userId}:${id}`;
  const write = async (work) => {
    if (uncertain || activePurchaseWrites.has(attemptKey))
      throw new Error("Confira a operação anterior antes de continuar.");
    activePurchaseWrites.add(attemptKey);
    saveDraft(attemptKey, true);
    try {
      await work();
      removeDraft(attemptKey);
    } catch (issue) {
      if ([400, 401, 403, 404, 409, 422].includes(issue.status)) removeDraft(attemptKey);
      else setUncertain(true);
      throw issue;
    } finally {
      activePurchaseWrites.delete(attemptKey);
    }
  };
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
  const changed = (message) => {
    setFlow(null);
    action.succeed(message);
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
        if (
          !has("purchases.update") ||
          !has(`purchases.${{ SENT: "send", CONFIRMED: "confirm", CANCELED: "cancel" }[status]}`)
        )
          throw new Error("Sem permissão para esta operação.");
        await write(() => purchases.update(id, { status }));
        refresh();
      },
      message,
      {
        title: label,
        confirmLabel: label,
        destructive: status === "CANCELED",
        successMessage: {
          SENT: `Envio da compra #${id} registrado.`,
          CONFIRMED: `Compra #${id} confirmada.`,
          CANCELED: `Compra #${id} cancelada.`,
        }[status],
      },
    );
  let pending = null;
  let receiptStorageError = false;
  try {
    pending = pendingReceipt(session.userId, id);
  } catch {
    receiptStorageError = true;
  }
  return (
    <Container
      hideTitle={embedded}
      title={`Detalhes da compra #${id}`}
      onClose={onClose}
      busy={action.pending}
      wide
      className="purchase-dialog purchase-details-dialog"
    >
      <ErrorAlert message={action.error} />
      {uncertain && (
        <section className="panel" role="alert">
          <p>
            Resultado da gravação desconhecido. Confira o estado atual antes de executar outra ação.
          </p>
          <button className="secondary" onClick={refresh}>
            Consultar estado
          </button>
          <button
            className="secondary"
            disabled={activePurchaseWrites.has(attemptKey)}
            onClick={async () => {
              if (activePurchaseWrites.has(attemptKey)) return;
              if (
                await confirm("Você conferiu o resultado atual da compra?", {
                  title: "Conferir resultado",
                  confirmLabel: "Conferi o resultado",
                })
              )
                setUncertain(false);
            }}
          >
            Conferi o resultado
          </button>
        </section>
      )}
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
              <button
                className="secondary"
                disabled={action.pending || uncertain || flow?.type === "receive"}
                onClick={refresh}
              >
                Atualizar
              </button>
              {purchase.status === "DRAFT" && has("purchases.update") && (
                <button
                  className="secondary"
                  disabled={action.pending || uncertain || flow?.type === "receive"}
                  onClick={onEdit}
                >
                  Editar compra
                </button>
              )}
              {purchase.status === "DRAFT" && has("purchases.create") && (
                <button
                  disabled={action.pending || uncertain || flow?.type === "receive"}
                  onClick={onEdit}
                >
                  <Plus size={17} /> Adicionar item
                </button>
              )}
              {purchase.status === "DRAFT" && canChangeStatus && has("purchases.send") && (
                <button
                  disabled={action.pending || uncertain || flow?.type === "receive"}
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
                  disabled={action.pending || uncertain || flow?.type === "receive"}
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
                    type="button"
                    className="purchase-cancel-action"
                    disabled={action.pending || uncertain || flow?.type === "receive"}
                    onClick={() =>
                      transition(
                        "CANCELED",
                        `Cancelar a compra #${id} ainda sem recebimento?`,
                        "Cancelar compra",
                      )
                    }
                  >
                    Cancelar compra
                  </button>
                )}
              {(pending ||
                receiptStorageError ||
                ["CONFIRMED", "PARTIALLY_RECEIVED"].includes(purchase.status)) &&
                has("purchases.receive") && (
                  <button
                    disabled={action.pending || uncertain || flow?.type === "receive"}
                    onClick={() => setFlow({ type: "receive" })}
                  >
                    {pending ? "Retomar recebimento" : "Receber em BASE"}
                  </button>
                )}
              {has("stock.read") && (
                <>
                  <button
                    className="secondary"
                    disabled={action.pending || uncertain || flow?.type === "receive"}
                    onClick={() => setFlow({ type: "stock" })}
                  >
                    Consultar saldos
                  </button>
                  <button
                    className="secondary"
                    disabled={action.pending || uncertain || flow?.type === "receive"}
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
                                  disabled={action.pending || uncertain || flow?.type === "receive"}
                                  onClick={onEdit}
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
                                  disabled={action.pending || uncertain || flow?.type === "receive"}
                                  onClick={() =>
                                    action.run(
                                      async () => {
                                        if (!has("purchases.delete"))
                                          throw new Error("Sem permissão para remover itens.");
                                        await write(() => purchases.deleteItem(item.id));
                                        refresh();
                                      },
                                      "Remover este item da compra em rascunho?",
                                      {
                                        title: "Remover item",
                                        confirmLabel: "Remover",
                                        destructive: true,
                                        successMessage: `Item #${item.id} removido da compra.`,
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
        <button
          type="button"
          className="secondary"
          disabled={action.pending || uncertain || flow?.type === "receive"}
          onClick={leave}
        >
          Fechar
        </button>
      </div>
      {flow?.type === "receive" && purchase && has("purchases.receive") && (
        <PurchaseReceiptForm
          embedded={embedded}
          closeRef={receiptClose}
          purchase={purchase}
          items={state.data.items}
          onClose={() => setFlow(null)}
          onSaved={() => changed("Recebimento da compra registrado.")}
        />
      )}
      {flow?.type === "history" && has("stock.read") && (
        <StockHistory filter={{ purchaseId: id }} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "stock" && has("stock.read") && (
        <DocumentStock items={state.data.items} revision={revision} onClose={() => setFlow(null)} />
      )}
    </Container>
  );
}
