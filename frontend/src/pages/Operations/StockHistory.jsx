import Modal from "../../components/feedback/Modal";
import { stockOperations } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import { quantity } from "../Products/productFormData";
import { formatRecordDateTime } from "../../utils/dateTime";
import "./stock.css";

const types = { INBOUND: "Entrada", OUTBOUND: "Saída", ADJUSTMENT: "Ajuste" };
const reasons = {
  PURCHASE: "Compra",
  SALE: "Venda",
  RETURN: "Devolução",
  MANUAL_ADJUSTMENT: "Manual",
  LOSS: "Perda",
};
export default function StockHistory({ filter, onClose }) {
  const state = useProductResource(
    (options) => stockOperations.history(filter, options),
    [JSON.stringify(filter)],
  );
  return (
    <Modal title="Histórico de movimentos · BASE" onClose={onClose} wide className="stock-dialog">
      <div className="stock-dialog-heading">
        <span>
          {state.loading
            ? "Carregando movimentos…"
            : state.data
              ? `${state.data.length} ${state.data.length === 1 ? "movimento registrado" : "movimentos registrados"}`
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
                {state.data?.map((movement) => (
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
      <div className="actions">
        <button type="button" className="secondary" onClick={onClose}>
          Fechar
        </button>
      </div>
    </Modal>
  );
}
