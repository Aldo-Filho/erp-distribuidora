import Modal from "../../components/feedback/Modal";
import { productStock } from "../../services/productStock";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import { quantity } from "../Products/productFormData";
import { available } from "./StockPage";
import "./stock.css";

export default function DocumentStock({ items, revision, onClose }) {
  const identities = [...new Set(items.map((item) => item.product.id))];
  const state = useProductResource(
    async (options) => {
      const rows = (
        await Promise.all(identities.map((id) => productStock.search(id, options)))
      ).flat();
      return rows.filter((stock) =>
        items.some(
          (item) =>
            item.product.id === stock.product.id && item.warehouse.id === stock.warehouse.id,
        ),
      );
    },
    [JSON.stringify(items.map((item) => [item.product.id, item.warehouse.id])), revision],
  );
  return (
    <Modal
      title="Saldos e reservas nos depósitos · BASE"
      onClose={onClose}
      wide
      className="stock-dialog"
    >
      <div className="stock-dialog-heading">
        <span>
          {state.loading
            ? "Carregando saldos…"
            : state.data
              ? `${state.data.length} ${state.data.length === 1 ? "item encontrado" : "itens encontrados"}`
              : "Total indisponível"}
        </span>
        <button type="button" className="secondary" onClick={state.reload}>
          Atualizar saldos
        </button>
      </div>
      <section
        className="panel table-panel"
        aria-label="Saldos nos depósitos"
        aria-busy={Boolean(state.loading)}
      >
        <ProductLoad state={state} empty="Sem estoque configurado nos depósitos dos itens.">
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>SKU / depósito</th>
                  <th>Físico</th>
                  <th>Reservado total</th>
                  <th>Disponível</th>
                </tr>
              </thead>
              <tbody>
                {state.data?.map((stock) => (
                  <tr key={stock.id}>
                    <td>
                      <strong>
                        {stock.product.sku} · {stock.warehouse.name}
                      </strong>
                      <small>{stock.product.name}</small>
                    </td>
                    <td>
                      {quantity(stock.quantity)} {stock.product.unit}
                    </td>
                    <td className="stock-quantity-reserved">{quantity(stock.reservedQuantity)}</td>
                    <td className="stock-quantity-available">
                      <strong>{available(stock)}</strong>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </ProductLoad>
      </section>
      <p className="field-hint">
        A reserva apresentada é a soma de todos os pedidos neste estoque.
      </p>
      <div className="actions">
        <button type="button" className="secondary" onClick={onClose}>
          Fechar
        </button>
      </div>
    </Modal>
  );
}
