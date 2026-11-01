import useListReturn from "../../hooks/useListReturn";
import { useEffect, useState } from "react";
import { Eye, Pencil, Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import Pagination from "../../components/feedback/Pagination";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import FilterSelect from "../../components/feedback/FilterSelect";
import RowActions from "../../components/feedback/RowActions";
import { purchases } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { money } from "../Products/productFormData";
import { partyName } from "./OperationSelectors";
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
  const context = useListReturn({ modulePath: "/purchases" });
  const [status, setStatusState] = useState(() =>
    statuses[context.restored?.status] ? context.restored.status : "",
  );
  const setStatus = (next) => {
    setPage(0);
    setStatusState(next);
  };
  const [page, setPage] = useState(() =>
    Number.isSafeInteger(context.restored?.page) && context.restored.page >= 0
      ? context.restored.page
      : 0,
  );
  const notice = "";
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
  const listReturn = useListReturn({
    modulePath: "/purchases",
    listState: { status, page },
    ready: Boolean(records),
  });
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
          <button className="customer-icon-label" onClick={() => listReturn.open("/purchases/new")}>
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
                            title="Detalhes"
                            onClick={() => listReturn.open(`/purchases/${purchase.id}`)}
                          >
                            <Eye size={17} />
                          </button>
                          {purchase.status === "DRAFT" && has("purchases.update") && (
                            <button
                              type="button"
                              className="secondary icon-button"
                              aria-label={`Editar compra ${purchase.id}`}
                              title="Editar"
                              onClick={() => listReturn.open(`/purchases/${purchase.id}/edit`)}
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
      <Pagination page={page} totalPages={state.data?.records?.totalPages || 0} change={setPage} />
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
