import { useState } from "react";
import { ChevronDown, ChevronUp, Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { products } from "../../services/products";
import { productSuppliers } from "../../services/productSuppliers";
import { useCatalogPage, useProductResource } from "../Products/useProductResource";
import { ProductLoad, ProductPageFooter } from "../Products/ProductControls";
import { skuName } from "./OperationSelectors";

export default function PurchaseProductCatalog(props) {
  const { has } = useAuth();
  if (!has("products.read") || !has("suppliers.read"))
    return (
      <section className="panel order-entry-catalog" id="purchase-catalog">
        <h2>Adicionar produtos</h2>
        <p>É necessário acesso a produtos e fornecedores para consultar opções de compra.</p>
        {props.editor}
      </section>
    );
  return <Catalog {...props} />;
}

function Catalog({ supplierId, disabled, onSelect, editor, selectedCatalogId }) {
  const list = useCatalogPage(products.search, { query: "", active: "true" });
  const [expanded, setExpanded] = useState(null);
  return (
    <section
      className="panel order-entry-catalog"
      id="purchase-catalog"
      aria-label="Catálogo para compra"
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
          onChange={(e) => list.changeFilters({ ...list.filters, query: e.target.value })}
        />
      </label>
      {!supplierId && (
        <p className="field-hint">Inicie a compra para escolher as opções do fornecedor.</p>
      )}
      {editor}
      <ProductLoad state={list}>
        {list.data?.content.map((catalog) => (
          <div
            className={`order-entry-product${selectedCatalogId === catalog.id ? " is-selected" : ""}`}
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
              </div>
              <button
                className="secondary icon-button"
                type="button"
                title="Ver opções de compra"
                aria-label={`Ver opções de ${catalog.name}`}
                aria-expanded={expanded === catalog.id}
                disabled={!supplierId}
                onClick={() => setExpanded(expanded === catalog.id ? null : catalog.id)}
              >
                {expanded === catalog.id ? (
                  <ChevronUp size={18} />
                ) : catalog.type === "SIMPLE" ? (
                  <Plus size={18} />
                ) : (
                  <ChevronDown size={18} />
                )}
              </button>
            </div>
            {expanded === catalog.id && (
              <Skus
                catalog={catalog}
                supplierId={supplierId}
                disabled={disabled}
                onSelect={onSelect}
              />
            )}
          </div>
        ))}
      </ProductLoad>
      <ProductPageFooter list={list} />
    </section>
  );
}
function Skus({ catalog, supplierId, disabled, onSelect }) {
  const [page, setPage] = useState(0);
  const state = useProductResource(
    (options) => products.items(catalog.id, { active: true }, page, 10, options),
    [catalog.id, page],
  );
  return (
    <ProductLoad state={state}>
      {state.data?.content.map((product) => (
        <Options
          key={`${supplierId}:${product.id}`}
          product={product}
          supplierId={supplierId}
          disabled={disabled}
          onSelect={onSelect}
        />
      ))}
      {state.data?.totalPages > 1 && (
        <div className="actions">
          <button
            type="button"
            className="secondary"
            disabled={!page}
            onClick={() => setPage(page - 1)}
          >
            Anterior
          </button>
          <span>{page + 1}</span>
          <button
            type="button"
            className="secondary"
            disabled={page + 1 >= (state.data?.totalPages || 0)}
            onClick={() => setPage(page + 1)}
          >
            Próxima
          </button>
        </div>
      )}
    </ProductLoad>
  );
}
function Options({ product, supplierId, disabled, onSelect }) {
  const [page, setPage] = useState(0);
  const [amount, setAmount] = useState("1");
  const state = useProductResource(
    async (options) => {
      const links = await productSuppliers.search(
        { productId: product.id, supplierId, active: true },
        options,
      );
      if (!links.length) return { content: [], totalPages: 0 };
      return productSuppliers.options(links[0].id, { active: true }, page, 10, options);
    },
    [supplierId, product.id, page],
    Boolean(supplierId),
  );
  return (
    <div className="purchase-catalog-sku">
      <strong>{skuName(product)}</strong>
      <ProductLoad state={state} empty="Sem opção ativa utilizável para este fornecedor.">
        {state.data?.content.map((option) => (
          <div className="purchase-option" key={option.id}>
            <span>
              {option.name} · {option.baseFactor} {product.unit} BASE
              {!option.conversionConfirmed && " · Conversão pendente"}
            </span>
            <label>
              Qtd.
              <input
                inputMode="decimal"
                aria-label={`Quantidade de ${product.sku} em ${option.name}`}
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </label>
            <button
              type="button"
              disabled={disabled || !option.conversionConfirmed}
              onClick={() =>
                onSelect({
                  product,
                  supplierOption: option,
                  quantity: amount,
                  key: `${product.id}:${option.id}`,
                })
              }
            >
              <Plus size={16} /> Adicionar
            </button>
          </div>
        ))}
        {state.data?.totalPages > 1 && (
          <div className="actions">
            <button className="secondary" disabled={!page} onClick={() => setPage(page - 1)}>
              Anterior
            </button>
            <button
              className="secondary"
              disabled={page + 1 >= state.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Próxima
            </button>
          </div>
        )}
      </ProductLoad>
    </div>
  );
}
