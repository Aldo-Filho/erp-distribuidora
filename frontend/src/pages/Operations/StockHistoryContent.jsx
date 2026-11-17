import { types, reasons } from "./stockHistoryLabels.js";
import { useEffect, useState } from "react";
import Pagination from "../../components/feedback/Pagination";
import { stockOperations } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import { quantity } from "../Products/productFormData";
import { formatRecordDateTime } from "../../utils/dateTime";
import "./stock.css";

export default function StockHistoryContent({ filter, onStateChange }) {
  const filterKey = JSON.stringify(filter);
  const [pagination, setPagination] = useState(() => ({ filterKey, page: 0 }));
  if (pagination.filterKey !== filterKey) setPagination({ filterKey, page: 0 });
  const page = pagination.filterKey === filterKey ? pagination.page : 0;
  const state = useProductResource(
    (options) => stockOperations.history({ ...filter, page, size: 25 }, options),
    [filterKey, page],
  );
  if (state.data && page > Math.max(0, state.data.totalPages - 1)) {
    setPagination({ filterKey, page: Math.max(0, state.data.totalPages - 1) });
  }
  useEffect(() => {
    onStateChange?.({ total: state.data?.totalElements, loading: Boolean(state.loading) });
  }, [state.data, state.loading, onStateChange]);
  return (
    <div className="stock-history-content">
      <div className="stock-dialog-heading">
        <span>
          {state.loading
            ? "Carregando movimentos…"
            : state.data
              ? `${state.data.totalElements} ${state.data.totalElements === 1 ? "movimento registrado" : "movimentos registrados"}`
              : "Total indisponível"}
        </span>
        <button type="button" className="secondary" onClick={state.reload}>
          Atualizar histórico
        </button>
      </div>
      <section
        className="panel table-panel"
        aria-label="Histórico de movimentos"
        aria-busy={Boolean(state.loading)}
      >
        <ProductLoad state={state} empty="Nenhum movimento registrado.">
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Movimento</th>
                  <th>Produto / depósito</th>
                  <th>Quantidade</th>
                  <th>Saldo anterior → novo</th>
                  <th>Documento</th>
                </tr>
              </thead>
              <tbody>
                {state.data?.content.map((movement) => (
                  <tr key={movement.id}>
                    <td>
                      <strong>
                        #{movement.id} · {types[movement.type]}
                      </strong>
                      <small>
                        {reasons[movement.reason]} · {formatRecordDateTime(movement.occurredAt)}
                      </small>
                      <small title={movement.notes || undefined}>{movement.notes}</small>
                    </td>
                    <td>
                      <strong>{movement.stockItem?.product?.sku}</strong>
                      <small>{movement.stockItem?.warehouse?.name}</small>
                    </td>
                    <td>
                      <strong>{quantity(movement.quantity)}</strong>{" "}
                      {movement.stockItem?.product?.unit}
                    </td>
                    <td>
                      {quantity(movement.previousBalance)} → {quantity(movement.newBalance)}
                    </td>
                    <td>
                      {movement.orderId
                        ? `Pedido #${movement.orderId}`
                        : movement.purchaseId
                          ? `Compra #${movement.purchaseId}`
                          : "Manual"}
                      <small>Operador #{movement.userId}</small>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </ProductLoad>
      </section>
      <Pagination
        page={page}
        totalPages={state.data?.totalPages}
        change={(next) => setPagination({ filterKey, page: next })}
      />
    </div>
  );
}
