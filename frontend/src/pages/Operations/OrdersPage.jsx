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
import { orders } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductForm from "../Products/useProductForm";
import useProductAction from "../Products/useProductAction";
import { money, quantity } from "../Products/productFormData";
import { PartyPicker, partyName } from "./OperationSelectors";
import TradeItemForm from "./TradeItemForm";
import StockHistory from "./StockHistory";
import DocumentStock from "./DocumentStock";
import { salesError } from "./salesErrors";
import "../Products/products.css";
import "./operations.css";
import "./orders.css";

export const orderStatuses = {
  PENDING: "Pendente",
  CONFIRMED: "Confirmado / reservado",
  INVOICED: "Despachado",
  CANCELED: "Cancelado",
};
const payments = {
  CASH: "Dinheiro",
  CREDIT_CARD: "Cartão de crédito",
  DEBIT_CARD: "Cartão de débito",
  BOLETO: "Boleto (somente registro)",
  PIX: "Pix",
  BANK_TRANSFER: "Transferência bancária",
};

export default function OrdersPage() {
  const { has } = useAuth();
  const [status, setStatusState] = useState("");
  const setStatus = (next) => { setPage(0); setStatusState(next); };
  const [paymentMethod, setPaymentMethodState] = useState("");
  const setPaymentMethod = (next) => { setPage(0); setPaymentMethodState(next); };
  const [page, setPage] = useState(0);
  const [flow, setFlow] = useState(null);
  const [notice, setNotice] = useState("");
  const filterKey = `${status}:${paymentMethod}`;
  const state = useProductResource(
    async (options) => ({
      filterKey,
      page,
      records: await orders.search({ status, paymentMethod, page, size: 20 }, options),
    }),
    [status, paymentMethod, page],
  );
  const loading = Boolean(
    state.loading ||
      (state.data && (state.data.filterKey !== filterKey || state.data.page !== page)),
  );
  useEffect(() => {
    if (loading || state.error || !state.data) return;
    const lastPage = Math.max(0, state.data.records.totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }, [loading, state.error, state.data, page]);
  const records = loading || state.error ? null : state.data?.records?.content;
  const filtered = Boolean(status || paymentMethod);
  return (
    <div className="customers-page operation-documents orders-page">
      <header className="page-title">
        <PageHeading
          title="Vendas"
          total={state.data?.records?.totalElements}
          loading={loading}
          singular={filtered ? "pedido encontrado" : "pedido cadastrado"}
          plural={filtered ? "pedidos encontrados" : "pedidos cadastrados"}
        />
        {has("orders.create") && (
          <button className="customer-icon-label" onClick={() => setFlow({ type: "new" })}>
            <Plus size={17} /> Nova venda
          </button>
        )}
      </header>
      <section className="panel customer-filters" aria-label="Filtros de vendas">
        <label>
          Situação
          <FilterSelect value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todas</option>
            {Object.entries(orderStatuses).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </FilterSelect>
        </label>
        <label>
          Forma de pagamento
          <FilterSelect value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
            <option value="">Todas</option>
            {Object.entries(payments).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </FilterSelect>
        </label>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setStatus("");
            setPaymentMethod("");
          }}
        >
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={!loading && salesError(state.error)} retry={state.reload} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <section
        className="panel table-panel orders-list"
        aria-label="Lista de vendas"
        aria-busy={loading}
      >
        {loading ? (
          <p role="status">Carregando pedidos…</p>
        ) : state.error ? (
          <p>Não foi possível carregar os pedidos.</p>
        ) : !records?.length ? (
          <p className="empty">
            {filtered
              ? "Nenhum pedido encontrado para estes filtros."
              : "Nenhum pedido cadastrado."}
          </p>
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Pedido</th>
                    <th>Cliente</th>
                    <th>Situação</th>
                    <th>Total</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((order) => (
                    <tr key={order.id}>
                      <td>
                        <strong>#{order.id}</strong>
                        <small>{payments[order.paymentMethod] || "Pagamento não informado"}</small>
                      </td>
                      <td>
                        <strong className="order-customer-name" title={partyName(order.customer)}>
                          {partyName(order.customer)}
                        </strong>
                      </td>
                      <td>
                        <OrderStatus status={order.status} />
                      </td>
                      <td>
                        <strong>{money(order.totalAmount)}</strong>
                      </td>
                      <td>
                        <RowActions
                          className="customer-row-actions"
                          label={`Ações do pedido ${order.id}`}
                        >
                          <button
                            type="button"
                            className="secondary icon-button"
                            aria-label={`Detalhes do pedido ${order.id}`}
                            title="Consultar pedido"
                            onClick={() => setFlow({ type: "details", id: order.id })}
                          >
                            <Eye size={17} />
                          </button>
                          {order.status === "PENDING" && has("orders.update") && (
                            <button
                              type="button"
                              className="secondary icon-button"
                              aria-label={`Editar pedido ${order.id}`}
                              title="Editar pedido"
                              onClick={() =>
                                setFlow({ type: "details", id: order.id, editing: true })
                              }
                            >
                              <Pencil size={17} />
                            </button>
                          )}
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
        <OrderHeaderForm
          onClose={() => setFlow(null)}
          onSaved={(order) => {
            setNotice(
              `Pedido #${order.id} criado como pendente. Adicione os produtos para continuar.`,
            );
            state.reload();
            setFlow({ type: "details", id: order.id, editing: true });
          }}
        />
      )}
      {flow?.type === "details" && (
        <OrderDetails
          id={flow.id}
          initialEditing={flow.editing}
          onClose={() => setFlow(null)}
          onChanged={state.reload}
        />
      )}
    </div>
  );
}
function OrderStatus({ status }) {
  return (
    <span className={`badge order-status order-status-${status?.toLowerCase()}`}>
      {orderStatuses[status] || status}
    </span>
  );
}
function OrderHeaderForm({ order, onClose, onSaved }) {
  const { has, session } = useAuth();
  const [picker, setPicker] = useState(false);
  const form = useProductForm(
    `school:order:${order?.id || "new"}`,
    { customer: order?.customer || null, paymentMethod: order ? order.paymentMethod || "" : "PIX" },
    onClose,
    onSaved,
  );
  return (
    <Modal
      title={order ? "Alterar cliente e pagamento" : "Nova venda"}
      onClose={form.close}
      busy={form.saving}
      wide
      className="order-dialog"
    >
      <form
        className="customer-form"
        onSubmit={(e) =>
          form.submit(e, (value) => {
            if (!value.customer) throw new Error("Selecione um cliente.");
            if (!order)
              return orders.register({
                customerId: value.customer.id,
                paymentMethod: value.paymentMethod || null,
                userId: session.userId,
              });
            const body = {};
            if (value.customer.id !== order.customer.id) body.customerId = value.customer.id;
            if (value.paymentMethod !== (order.paymentMethod || ""))
              body.paymentMethod = value.paymentMethod;
            if (!Object.keys(body).length) return undefined;
            return orders.update(order.id, body);
          })
        }
      >
        <ErrorAlert message={salesError(form.error)} />
        <fieldset
          disabled={form.saving || !has(order ? "orders.update" : "orders.create")}
          className="customer-form-fields"
        >
          <div className="form-grid">
            <label className="span-all">
              Cliente
              <div className="order-customer-control">
                <input
                  readOnly
                  value={form.value.customer ? partyName(form.value.customer) : ""}
                  placeholder="Selecione um cliente"
                />
                <button
                  type="button"
                  className="secondary"
                  disabled={!has("customers.read")}
                  onClick={() => setPicker(true)}
                >
                  {form.value.customer ? "Trocar cliente" : "Selecionar cliente"}
                </button>
              </div>
            </label>
            <label>
              Forma de pagamento
              <FilterSelect
                value={form.value.paymentMethod}
                onChange={(e) => form.change("paymentMethod", e.target.value)}
              >
                {order && !order.paymentMethod && (
                  <option value="" disabled>
                    Não informado
                  </option>
                )}
                {Object.entries(payments).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </FilterSelect>
            </label>
            <p className="field-hint span-all">
              {order && form.value.customer?.id !== order.customer.id
                ? "Alterar o cliente não recalcula os preços já salvos nos itens. Confira os valores antes de confirmar."
                : order
                  ? "Estas alterações são salvas neste pedido pendente."
                  : "Ao continuar, a venda será salva como pendente. Você poderá adicionar os produtos em seguida."}
            </p>
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            {order ? "Voltar ao pedido" : "Cancelar"}
          </button>
          <button
            disabled={
              form.saving ||
              !form.value.customer ||
              !has(order ? "orders.update" : "orders.create") ||
              (order && !form.dirty)
            }
          >
            {form.saving
              ? "Salvando…"
              : order
                ? "Salvar alterações"
                : "Continuar para produtos"}
          </button>
        </div>
      </form>
      {picker && (
        <PartyPicker
          onClose={() => setPicker(false)}
          onSelect={(customer) => {
            form.change("customer", customer);
            setPicker(false);
          }}
        />
      )}
    </Modal>
  );
}
function OrderDetails({ id, initialEditing = false, onClose, onChanged }) {
  const { has } = useAuth();
  const [flow, setFlow] = useState(null);
  const [editing, setEditing] = useState(initialEditing);
  const [revision, setRevision] = useState(0);
  const [notice, setNotice] = useState("");
  const action = useProductAction();
  const state = useProductResource(
    async (options) => {
      const [order, items] = await Promise.all([
        orders.findOne(id, options),
        orders.items(id, options),
      ]);
      return order ? { order, items } : null;
    },
    [id, revision],
  );
  const refresh = () => {
    setRevision((n) => n + 1);
    onChanged();
  };
  const changed = (message) => {
    setFlow(null);
    action.succeed();
    setNotice(message);
    refresh();
  };
  const order = state.data?.order;
  const items = state.data?.items || [];
  const pending = order?.status === "PENDING";
  const canEdit = pending && (has("orders.update") || has("orders.create") || has("orders.delete"));
  const editable = canEdit && editing;
  const showItemActions = editable && (has("orders.update") || has("orders.delete"));
  const canConfirm = pending && has("orders.update") && has("orders.confirm") && items.length > 0;
  const canCancel =
    ["PENDING", "CONFIRMED"].includes(order?.status) &&
    has("orders.update") &&
    has("orders.cancel");
  const canDispatch = order?.status === "CONFIRMED" && has("orders.dispatch");
  const transition = (status, message, label) =>
    action.run(
      async () => {
        setNotice("");
        try {
          await orders.update(id, { status });
        } catch (issue) {
          let current;
          try {
            current = await orders.findOne(id);
          } catch {
            throw new Error(
              "Não foi possível confirmar o resultado da operação. " +
                "A consulta do pedido também falhou. Atualize o pedido antes de tentar novamente.",
            );
          }

          refresh();
          if (current?.status !== status) {
            throw new Error(
              current?.status === (status === "CONFIRMED" ? "PENDING" : "CONFIRMED") ||
              (status === "CANCELED" && current?.status === "PENDING")
                ? "O pedido ainda apresenta o status anterior, mas o resultado da operação " +
                    "não é conclusivo. Atualize o pedido antes de tentar novamente."
                : "Não foi possível confirmar a operação. Confira a situação atual " +
                    "do pedido antes de tentar novamente.",
            );
          }
        }
        setEditing(false);
        setNotice(
          status === "CONFIRMED"
            ? "Pedido confirmado. Os itens foram reservados no estoque."
            : "Pedido cancelado. As reservas existentes foram liberadas.",
        );
        refresh();
      },
      message,
      { title: label, confirmLabel: label, destructive: status === "CANCELED" },
    );
  return (
    <Modal
      title={`${editable ? "Editar venda" : "Detalhes da venda"} #${id}`}
      onClose={onClose}
      busy={action.pending}
      wide
      className="order-dialog order-details-dialog"
    >
      <div
        className="order-details-content"
      >
        <ErrorAlert message={salesError(action.error)} />
        {notice && (
          <p className="success" role="status">
            {notice}
          </p>
        )}
        <ProductLoad state={state} empty="Pedido não encontrado.">
          {order && (
            <>
              <div className="customer-detail-title">
                <h3>{partyName(order.customer)}</h3>
                <OrderStatus status={order.status} />
              </div>
              <dl className="customer-details">
                <div>
                  <dt>Pedido</dt>
                  <dd>#{order.id}</dd>
                </div>
                <div>
                  <dt>Forma de pagamento</dt>
                  <dd>{payments[order.paymentMethod] || "Não informada"}</dd>
                </div>
                <div>
                  <dt>Total</dt>
                  <dd>
                    <strong>{money(order.totalAmount)}</strong>
                  </dd>
                </div>
              </dl>
              {pending && (
                <p className="field-hint order-flow-hint">
                  {!items.length
                    ? "Adicione os produtos para montar o pedido. A confirmação ficará disponível depois do primeiro item."
                    : editable
                      ? "Cada item é salvo ao adicionar ou editar. Confira o total antes de confirmar e reservar o estoque."
                      : "Este pedido ainda não reserva estoque. Confira os itens antes de confirmar."}
                </p>
              )}
              {order.status === "CONFIRMED" && (
                <p className="field-hint order-flow-hint">
                  Itens reservados. O próximo passo é despachar o pedido para registrar a saída do
                  estoque.
                </p>
              )}
              {order.status === "INVOICED" && (
                <p className="field-hint order-flow-hint">
                  Saída comercial registrada. Esta operação não emite NF-e.
                </p>
              )}
              <div className="order-consult-actions">
                <div className="order-secondary-actions">
                  {canEdit && !editable && items.length > 0 && (
                    <button
                      type="button"
                      className="secondary customer-icon-label"
                      disabled={action.pending}
                      onClick={() => setEditing(true)}
                    >
                      <Pencil size={17} /> Editar pedido
                    </button>
                  )}
                  {editable && has("orders.update") && (
                    <button
                      type="button"
                      className="secondary"
                      disabled={action.pending}
                      onClick={() => setFlow({ type: "header" })}
                    >
                      Alterar cliente e pagamento
                    </button>
                  )}
                  {has("stock.read") && items.length > 0 && (
                    <button
                      type="button"
                      className="secondary"
                      disabled={action.pending}
                      onClick={() => setFlow({ type: "stock" })}
                    >
                      {order.status === "CONFIRMED" ? "Saldos e reservas" : "Consultar estoque"}
                    </button>
                  )}
                  {has("stock.read") && order.status === "INVOICED" && (
                    <button
                      type="button"
                      className="secondary"
                      disabled={action.pending}
                      onClick={() => setFlow({ type: "history" })}
                    >
                      Histórico de saída
                    </button>
                  )}
                </div>
                <button
                  type="button"
                  className="secondary"
                  disabled={action.pending}
                  onClick={refresh}
                >
                  Atualizar pedido
                </button>
              </div>
              <div className="order-section-heading">
                <h3>Produtos da venda</h3>
                <div className="order-secondary-actions">
                  <span className="field-hint">
                    {items.length} {items.length === 1 ? "item" : "itens"}
                  </span>
                  {editable && has("orders.create") && items.length > 0 && (
                    <button
                      type="button"
                      className="secondary customer-icon-label"
                      disabled={action.pending}
                      onClick={() => setFlow({ type: "item" })}
                    >
                      <Plus size={17} /> Adicionar produto
                    </button>
                  )}
                </div>
              </div>
              <section className="panel table-panel" aria-label="Itens do pedido">
                {items.length > 0 ? (
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Produto / apresentação</th>
                          <th>Depósito</th>
                          <th>Comercial / BASE</th>
                          <th>Preço / desconto</th>
                          {showItemActions && <th>Ações</th>}
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((item) => (
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
                              {quantity(item.quantity)} comercial
                              <small>
                                {quantity(item.baseQuantity)} {item.unitSnapshot || "BASE pendente"}
                              </small>
                            </td>
                            <td>
                              {money(item.unitPrice)}
                              <small>
                                Desconto:{" "}
                                {item.discountType === "PERCENTAGE"
                                  ? `${quantity(item.discount)}%`
                                  : money(item.discount)}
                              </small>
                            </td>
                            {showItemActions && (
                              <td>
                                <RowActions
                                  className="customer-row-actions"
                                  label={`Ações do item ${item.id}`}
                                >
                                  {has("orders.update") && (
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
                                  {has("orders.delete") && (
                                    <button
                                      type="button"
                                      className="secondary icon-button danger-link"
                                      aria-label={`Remover item ${item.id}`}
                                      title="Remover item"
                                      disabled={action.pending}
                                      onClick={() =>
                                        action.run(
                                          async () => {
                                            setNotice("");
                                            await orders.deleteItem(item.id);
                                            setNotice("Produto removido do pedido.");
                                            refresh();
                                          },
                                          "Remover este item do pedido pendente?",
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
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="order-empty-items">
                    <p>Nenhum produto adicionado.</p>
                    {editable && has("orders.create") ? (
                      <button
                        type="button"
                        className="customer-icon-label"
                        disabled={action.pending}
                        onClick={() => setFlow({ type: "item" })}
                      >
                        <Plus size={17} /> Adicionar primeiro produto
                      </button>
                    ) : canEdit && !editable ? (
                      <button
                        type="button"
                        className="secondary"
                        disabled={action.pending}
                        onClick={() => setEditing(true)}
                      >
                        {has("orders.create") ? "Montar pedido" : "Editar dados do pedido"}
                      </button>
                    ) : (
                      <p className="field-hint">
                        É necessária permissão de adicionar itens para montar este pedido.
                      </p>
                    )}
                  </div>
                )}
              </section>
              <RecordMetadata record={order} />
            </>
          )}
        </ProductLoad>
      </div>
      <div className="order-footer-actions">
        <div>
          {canCancel && !state.loading && (
            <button
              type="button"
              className="secondary order-cancel-action"
              disabled={action.pending}
              onClick={() =>
                transition(
                  "CANCELED",
                  "Cancelar este pedido? As reservas existentes serão liberadas.",
                  "Cancelar pedido",
                )
              }
            >
              Cancelar pedido
            </button>
          )}
        </div>
        <div className="order-secondary-actions">
          <button type="button" className="secondary" disabled={action.pending} onClick={onClose}>
            {editable && pending ? "Fechar e manter pendente" : "Fechar"}
          </button>
          {canConfirm && !state.loading && (
            <button
              type="button"
              disabled={action.pending}
              onClick={() =>
                transition(
                  "CONFIRMED",
                  "Confirmar este pedido e reservar os itens no depósito?",
                  "Confirmar venda",
                )
              }
            >
              Confirmar venda
            </button>
          )}
          {canDispatch && !state.loading && (
            <button
              type="button"
              disabled={action.pending}
              onClick={() =>
                action.run(
                  async () => {
                    setNotice("");
                    try {
                      await orders.dispatch(id);
                    } catch (issue) {
                      let current;
                      try {
                        current = await orders.findOne(id);
                      } catch {
                        throw new Error(
                          "Não foi possível confirmar o resultado do despacho. " +
                            "A consulta do pedido também falhou. Atualize o pedido antes de tentar novamente.",
                        );
                      }

                      refresh();
                      if (current?.status !== "INVOICED") {
                        throw new Error(
                          current?.status === "CONFIRMED"
                            ? "O pedido ainda aparece como confirmado, mas o resultado do despacho " +
                                "não é conclusivo. Atualize o pedido antes de tentar novamente."
                            : "Não foi possível confirmar o despacho. Confira a situação atual " +
                                "do pedido antes de tentar novamente.",
                        );
                      }
                    }
                    setEditing(false);
                    setNotice("Pedido despachado. A saída dos itens foi registrada no estoque.");
                    refresh();
                  },
                  "Despachar este pedido e registrar a saída dos itens reservados?",
                  { title: "Despachar pedido", confirmLabel: "Despachar" },
                )
              }
            >
              Despachar pedido
            </button>
          )}
        </div>
      </div>
      {flow?.type === "header" && order && (
        <OrderHeaderForm
          order={order}
          onClose={() => setFlow(null)}
          onSaved={() => changed("Cliente e pagamento atualizados.")}
        />
      )}
      {flow?.type === "item" && order && (
        <TradeItemForm
          document={order}
          item={flow.item}
          existingItems={items}
          onClose={() => setFlow(null)}
          onSaved={() =>
            changed(flow.item ? "Produto atualizado no pedido." : "Produto adicionado ao pedido.")
          }
        />
      )}
      {flow?.type === "history" && (
        <StockHistory filter={{ orderId: id }} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "stock" && state.data && (
        <DocumentStock items={items} revision={revision} onClose={() => setFlow(null)} />
      )}
    </Modal>
  );
}
