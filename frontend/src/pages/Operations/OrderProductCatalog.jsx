import { useRef, useState } from "react";
import { ChevronDown, ChevronUp, Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import FilterSelect from "../../components/feedback/FilterSelect";
import Pagination from "../../components/feedback/Pagination";
import { products } from "../../services/products";
import { orders } from "../../services/operations";
import { useCatalogPage, useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import { money } from "../Products/productFormData";
import { skuName } from "./OperationSelectors";

let activeQuotes = 0;
const quoteQueue = [];
function scheduleQuote(read, signal) {
  return new Promise((resolve, reject) => {
    const start = async () => {
      activeQuotes += 1;
      try {
        if (signal?.aborted) throw new DOMException("Aborted", "AbortError");
        resolve(await read());
      } catch (issue) {
        reject(issue);
      } finally {
        activeQuotes -= 1;
        quoteQueue.shift()?.();
      }
    };
    if (activeQuotes < 4) start();
    else quoteQueue.push(start);
  });
}

export default function OrderProductCatalog(props) {
  const { has } = useAuth();
  if (!has("products.read"))
    return (
      <section className="panel order-entry-catalog">
        <h2>Produtos</h2>
        <p>
          Seu cargo não permite consultar o catálogo. Os itens existentes continuam acessíveis
          conforme suas permissões.
        </p>
        {props.editor}
      </section>
    );
  return <Catalog {...props} />;
}

function Catalog({ order, onSelect, disabled, editor, selectedCatalogId }) {
  const { has } = useAuth();
  const list = useCatalogPage(products.search, { query: "", active: "true" });
  const [expanded, setExpanded] = useState(null);
  const [selecting, setSelecting] = useState(null);
  const [error, setError] = useState("");
  const [quantities, setQuantities] = useState({});
  const cache = useRef(new Map());
  const lock = useRef(false);
  const select = async (catalog) => {
    if (lock.current || disabled) return;
    if (catalog.type !== "SIMPLE") {
      setExpanded(expanded === catalog.id ? null : catalog.id);
      return;
    }
    lock.current = true;
    setSelecting(catalog.id);
    setError("");
    try {
      let item = cache.current.get(catalog.id);
      if (!item) {
        const page = await products.items(catalog.id, { active: true }, 0, 2);
        if (page.totalElements !== 1 || page.content.length !== 1)
          throw new Error("Este produto não possui um único SKU ativo. Confira o cadastro.");
        item = page.content[0];
        if (cache.current.size >= 50) cache.current.clear();
        cache.current.set(catalog.id, item);
      }
      await onSelect(item, null, quantities[catalog.id] ?? "1");
    } catch (issue) {
      setError(issue.message);
      cache.current.delete(catalog.id);
    } finally {
      lock.current = false;
      setSelecting(null);
    }
  };
  return (
    <section
      className="panel order-entry-catalog"
      id="order-entry-products"
      aria-label="Catálogo para venda"
    >
      <div className="order-entry-section-title">
        <h2>Adicionar produtos</h2>
        <span className="field-hint">
          {list.data ? `${list.data.totalElements} encontrados` : ""}
        </span>
      </div>
      <label>
        Buscar produto
        <input
          type="search"
          placeholder="Nome, SKU ou característica"
          value={list.filters.query}
          onChange={(event) => list.changeFilters({ ...list.filters, query: event.target.value })}
        />
      </label>
      {error && (
        <p className="error-alert" role="alert">
          {error}
        </p>
      )}
      {!order && (
        <p className="field-hint">
          Inicie o pedido para adicionar produtos com o preço do cliente.
        </p>
      )}
      {editor}
      <ProductLoad state={list} empty="Nenhum produto encontrado.">
        <div className="order-entry-product-list">
          {list.data?.content.map((catalog) => (
            <div
              className={
                "order-entry-product" + (selectedCatalogId === catalog.id ? " is-selected" : "")
              }
              key={catalog.id}
            >
              <div className="order-entry-product-heading">
                <div>
                  <strong>{catalog.name}</strong>
                  <small>
                    {catalog.brand?.name || "Sem marca"} ·{" "}
                    {catalog.type === "SIMPLE" ? "Produto simples" : "Com variações"} ·{" "}
                    {catalog.unit || "Unidade pendente"}
                  </small>
                  <CatalogPrice catalog={catalog} order={order} cache={cache} />
                </div>
                {catalog.type === "SIMPLE" && (
                  <label className="order-entry-quantity">
                    Qtd.
                    <input
                      type="number"
                      min={catalog.unit === "UN" ? "1" : "0.1"}
                      step={catalog.unit === "UN" ? "1" : "0.1"}
                      aria-label={`Quantidade de ${catalog.name}`}
                      value={quantities[catalog.id] ?? "1"}
                      onChange={(event) =>
                        setQuantities((previous) => ({
                          ...previous,
                          [catalog.id]: event.target.value,
                        }))
                      }
                      disabled={disabled}
                    />
                  </label>
                )}
                <button
                  type="button"
                  className="secondary icon-button"
                  aria-label={`${catalog.type === "SIMPLE" ? "Selecionar" : "Ver variações de"} ${catalog.name}`}
                  disabled={disabled || selecting !== null || !order || !has("orders.create")}
                  onClick={() => select(catalog)}
                >
                  {catalog.type === "SIMPLE" ? (
                    <Plus size={18} />
                  ) : expanded === catalog.id ? (
                    <ChevronUp size={18} />
                  ) : (
                    <ChevronDown size={18} />
                  )}
                </button>
              </div>
              {selecting === catalog.id && (
                <p role="status" className="field-hint">
                  Carregando SKU…
                </p>
              )}
              {expanded === catalog.id && (
                <CatalogSkus catalog={catalog} disabled={disabled || !order} onSelect={onSelect} />
              )}
            </div>
          ))}
        </div>
      </ProductLoad>
      <div className="customer-list-footer">
        <label>
          Por página
          <FilterSelect
            value={list.size}
            onChange={(event) => list.changeSize(Number(event.target.value))}
          >
            {[25, 50, 100].map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </FilterSelect>
        </label>
        <Pagination page={list.page} totalPages={list.data?.totalPages} change={list.changePage} />
      </div>
    </section>
  );
}

function CatalogSkus({ catalog, onSelect, disabled }) {
  const [page, setPage] = useState(0);
  const state = useProductResource(
    (options) => products.items(catalog.id, { active: true }, page, 25, options),
    [catalog.id, page],
  );
  return (
    <div className="order-entry-skus">
      <ProductLoad state={state} empty="Nenhuma variação ativa.">
        {state.data?.content.map((item) => (
          <button
            key={item.id}
            className="secondary"
            type="button"
            disabled={disabled}
            onClick={() => onSelect(item)}
          >
            <span>{skuName(item)}</span>
            <small>Selecionar apresentação e quantidade</small>
          </button>
        ))}
      </ProductLoad>
      <Pagination page={page} totalPages={state.data?.totalPages} change={setPage} />
    </div>
  );
}

function CatalogPrice({ catalog, order, cache }) {
  const state = useProductResource(
    (options) =>
      scheduleQuote(async () => {
        let item = cache.current.get(catalog.id);
        if (!item) {
          const page = await products.items(catalog.id, { active: true }, 0, 2, options);
          if (page.totalElements !== 1 || page.content.length !== 1)
            throw new Error("SKU indisponível.");
          item = page.content[0];
          if (cache.current.size >= 50) cache.current.clear();
          cache.current.set(catalog.id, item);
        }
        return orders.price(order.id, item.id, undefined, options);
      }, options.signal),
    [catalog.id, order?.id, order?.customer?.id],
    Boolean(order) && catalog.type === "SIMPLE",
  );
  if (!order) return null;
  if (catalog.type !== "SIMPLE") return <small>Preço por variação e apresentação</small>;
  return (
    <small>
      {state.loading ? (
        "Consultando preço do cliente…"
      ) : state.error ? (
        <button type="button" className="text-button" onClick={state.reload}>
          Reconsultar preço
        </button>
      ) : state.data != null ? (
        `${money(state.data)} / ${catalog.unit || "BASE"}`
      ) : (
        "Preço indisponível"
      )}
    </small>
  );
}
