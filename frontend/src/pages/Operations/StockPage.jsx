import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { useEffect, useState } from "react";
import { Eye, Plus, Settings, ArrowDownToLine, History } from "lucide-react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import Pagination from "../../components/feedback/Pagination";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RowActions from "../../components/feedback/RowActions";
import { stockOperations } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import { quantity } from "../Products/productFormData";
import { SkuPicker, skuName } from "./OperationSelectors";
import StockHistory from "./StockHistory";
import { StockConfiguration, StockMovement } from "./StockActions";
import "../Products/products.css";
import "./operations.css";
import "./stock.css";
//* Difference is display-only. All persisted balances come from the API.*
export function available(stock) {
  const scaled = (value) => {
    const [whole, fraction = ""] = String(value).split(".");
    return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
  };
  const difference = scaled(stock.quantity) - scaled(stock.reservedQuantity);
  return `${difference / 10n}${difference % 10n ? "," + String(difference % 10n) : ""}`;
}
export default function StockPage() {
  const { has } = useAuth();
  const origin = useListReturn({ modulePath: "/products" });
  const [product, setProduct] = useState(() =>
    validId(origin.restored?.filters?.productId)
      ? { id: Number(origin.restored.filters.productId) }
      : null,
  );
  const [picker, setPicker] = useState(false);
  const [page, setPage] = useState(() =>
    Number.isSafeInteger(origin.restored?.page) && origin.restored.page >= 0
      ? origin.restored.page
      : 0,
  );
  const [flow, setFlow] = useState(null);
  const [notice, setNotice] = useState("");
  const state = useProductResource(
    async (options) => ({
      productId: product?.id || null,
      page,
      records: await stockOperations.search({ productId: product?.id, page, size: 20 }, options),
    }),
    [product?.id, page],
  );
  const loading = Boolean(
    state.loading ||
    (state.data && (state.data.productId !== (product?.id || null) || state.data.page !== page)),
  );
  useEffect(() => {
    if (loading || state.error || !state.data) return;
    const lastPage = Math.max(0, state.data.records.totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }, [loading, state.error, state.data, page]);
  const records = loading || state.error ? null : state.data?.records?.content;
  const listReturn = useListReturn({
    modulePath: "/products",
    listState: { filters: { productId: product?.id || "" }, page, size: 20 },
    ready: Boolean(records),
  });
  const productLabel = product ? (product.sku ? skuName(product) : `SKU #${product.id}`) : "";
  const saved = () => {
    setFlow(null);
    setNotice("Estoque atualizado.");
    state.reload();
  };
  return (
    <div className="customers-page operation-documents stock-page">
      <header className="page-title">
        <PageHeading
          title="Produtos"
          total={state.data?.records?.totalElements}
          loading={loading}
          singular={product ? "item encontrado" : "item configurado"}
          plural={product ? "itens encontrados" : "itens configurados"}
        />
        <div className="stock-page-actions">
          <button type="button" className="secondary customer-icon-label" onClick={state.reload}>
            Atualizar saldos
          </button>
          <button
            type="button"
            className="secondary customer-icon-label"
            onClick={() =>
              listReturn.open(`/stock/history${product ? `?productId=${product.id}` : ""}`)
            }
          >
            <History size={17} /> Histórico
          </button>
          {has("stock.configure") && (
            <button className="customer-icon-label" onClick={() => setFlow({ type: "configure" })}>
              <Plus size={17} /> Configurar item
            </button>
          )}
        </div>
      </header>
      <section className="panel customer-filters" aria-label="Filtros de estoque">
        <label className="customer-query">
          Produto / SKU
          <div className="stock-product-control">
            <input
              readOnly
              value={productLabel}
              title={productLabel || undefined}
              placeholder="Todos os produtos"
            />
            <button
              type="button"
              className="secondary"
              onClick={() => setPicker(true)}
              disabled={!has("products.read")}
            >
              {product ? "Trocar produto" : "Selecionar produto"}
            </button>
          </div>
        </label>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setPage(0);
            setProduct(null);
          }}
        >
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={!loading && state.error} retry={state.reload} />
      {notice && (
        <p role="status" className="success">
          {notice}
        </p>
      )}
      <section
        className="panel table-panel"
        aria-label="Lista de estoque por depósito"
        aria-busy={loading}
      >
        {loading ? (
          <p role="status">Carregando estoque…</p>
        ) : state.error ? (
          <p>Não foi possível carregar o estoque.</p>
        ) : !records?.length ? (
          <p className="empty">
            {product
              ? "Nenhum estoque configurado para este produto."
              : "Nenhum item configurado em depósito."}
          </p>
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Produto / SKU</th>
                    <th>Depósito</th>
                    <th>Físico</th>
                    <th>Reservado</th>
                    <th>Disponível</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((stock) => (
                    <tr key={stock.id}>
                      <td>
                        <strong>{stock.product.name}</strong>
                        <small title={skuName(stock.product)}>{skuName(stock.product)}</small>
                      </td>
                      <td>
                        {stock.warehouse.name}
                        <small>BASE: {stock.product.unit || "Unidade pendente"}</small>
                      </td>
                      <td>
                        <strong>{quantity(stock.quantity)}</strong>
                      </td>
                      <td className="stock-quantity-reserved">
                        {quantity(stock.reservedQuantity)}
                      </td>
                      <td className="stock-quantity-available">
                        <strong>{available(stock)}</strong>
                      </td>
                      <td>
                        <RowActions
                          className="customer-row-actions"
                          label={`Ações de ${stock.product.sku} em ${stock.warehouse.name}`}
                        >
                          <button
                            type="button"
                            className="secondary icon-button"
                            aria-label={`Histórico de ${stock.product.sku} em ${stock.warehouse.name}`}
                            title="Histórico"
                            onClick={() =>
                              setFlow({ type: "history", filter: { stockItemId: stock.id } })
                            }
                          >
                            <Eye size={17} />
                          </button>
                          {has("stock.configure") && (
                            <button
                              type="button"
                              className="secondary icon-button"
                              aria-label={`Configurar limites de ${stock.product.sku}`}
                              title="Configurar limites"
                              onClick={() => setFlow({ type: "configure", stock })}
                            >
                              <Settings size={17} />
                            </button>
                          )}
                          {has("stock.adjust") && (
                            <button
                              type="button"
                              className="secondary icon-button"
                              aria-label={`Movimentar estoque de ${stock.product.sku}`}
                              title="Movimentar estoque"
                              onClick={() => setFlow({ type: "move", stock })}
                            >
                              <ArrowDownToLine size={17} />
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
      {picker && (
        <SkuPicker
          onClose={() => setPicker(false)}
          onSelect={(item) => {
            setPage(0);
            setProduct(item);
            setPicker(false);
          }}
        />
      )}
      {flow?.type === "history" && (
        <StockHistory filter={flow.filter} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "configure" && (
        <StockConfiguration stock={flow.stock} onSaved={saved} onClose={() => setFlow(null)} />
      )}
      {flow?.type === "move" && (
        <StockMovement stock={flow.stock} onSaved={saved} onClose={() => setFlow(null)} />
      )}
    </div>
  );
}
