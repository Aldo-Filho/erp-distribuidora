import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import Pagination from "../../components/feedback/Pagination";
import { customers } from "../../services/customers";
import { suppliers } from "../../services/suppliers";
import { products } from "../../services/products";
import { orders } from "../../services/operations";
import { useCatalogPage, useProductResource } from "../Products/useProductResource";
import { ProductLoad, ProductPageFooter, ProductFilters } from "../Products/ProductControls";
import { characteristics, money } from "../Products/productFormData";

export const partyName = (record) => record?.tradeName || record?.legalName || "—";
export const skuName = (item) =>
  `${item.sku} · ${item.name}${
    Object.entries(item.attributes || {}).length
      ? " · " +
        Object.entries(item.attributes)
          .map(([key, value]) => `${characteristics[key] || key}: ${value}`)
          .join(" / ")
      : ""
  }`;
const readCustomers = (filter, page, size, options) =>
  customers.search({ ...filter, searchField: "NAME", active: true }, page, size, options);
const readSuppliers = (filter, page, size, options) =>
  suppliers.search({ ...filter, searchField: "NAME", active: true }, page, size, options);

export function PartyPicker({ purchase = false, onSelect, onClose }) {
  const { has } = useAuth();
  const allowed = has(purchase ? "suppliers.read" : "customers.read");
  return (
    <Modal
      title={purchase ? "Selecionar fornecedor" : "Selecionar cliente"}
      onClose={onClose}
      wide
      className={allowed ? "party-picker-dialog" : ""}
    >
      {allowed ? (
        <PartyResults purchase={purchase} onSelect={onSelect} />
      ) : (
        <p>É necessária permissão de consulta deste cadastro.</p>
      )}
    </Modal>
  );
}
function PartyResults({ purchase, onSelect }) {
  const list = useCatalogPage(purchase ? readSuppliers : readCustomers);
  return (
    <div className="party-picker-content">
      <label>
        Buscar por nome
        <input
          type="search"
          value={list.filters.query}
          onChange={(e) => list.changeFilters({ ...list.filters, query: e.target.value })}
        />
      </label>
      <div
        className="party-picker-results"
        role="region"
        aria-label={purchase ? "Fornecedores encontrados" : "Clientes encontrados"}
        aria-busy={Boolean(list.loading)}
        tabIndex={0}
      >
        <ProductLoad state={list}>
          <div className="operation-choices">
            {list.data?.content.map((party) => (
              <button
                type="button"
                className="secondary"
                key={party.id}
                onClick={() => onSelect(party)}
              >
                {partyName(party)} · #{party.id}
              </button>
            ))}
          </div>
        </ProductLoad>
      </div>
      <ProductPageFooter list={list} />
    </div>
  );
}

export function SkuPicker({ onSelect, onClose, orderId }) {
  const { has } = useAuth();
  const [catalog, setCatalog] = useState(null);
  return (
    <Modal
      title="Selecionar produto / SKU"
      onClose={onClose}
      wide
      className={has("products.read") ? "party-picker-dialog" : ""}
    >
      {!has("products.read") ? (
        <p>É necessária permissão de consulta de produtos.</p>
      ) : catalog ? (
        <>
          <button type="button" className="text-button" onClick={() => setCatalog(null)}>
            Voltar aos produtos
          </button>
          <SkuResults key={catalog.id} catalog={catalog} onSelect={onSelect} orderId={orderId} />
        </>
      ) : (
        <CatalogResults onSelect={setCatalog} orderId={orderId} />
      )}
    </Modal>
  );
}
export function SalesProductPicker({ onSelect, orderId }) {
  const { has } = useAuth();
  const [catalog, setCatalog] = useState(null);
  if (!has("products.read")) {
    return <p>É necessária permissão de consulta de produtos.</p>;
  }
  return (
    <section className="order-product-picker" aria-label="Escolher produto">
      {catalog ? (
        <>
          <button type="button" className="text-button" onClick={() => setCatalog(null)}>
            Voltar à lista de produtos
          </button>
          <SkuResults key={catalog.id} catalog={catalog} onSelect={onSelect} orderId={orderId} />
        </>
      ) : (
        <CatalogResults onSelect={setCatalog} orderId={orderId} compact />
      )}
    </section>
  );
}

function CatalogResults({ onSelect, orderId, compact = false }) {
  const list = useCatalogPage(products.search, { query: "", active: "true" });
  return (
    <div className="party-picker-content">
      {compact ? (
        <label>
          Buscar produto
          <input
            type="search"
            placeholder="Nome, SKU ou característica"
            value={list.filters.query}
            onChange={(event) => list.changeFilters({ ...list.filters, query: event.target.value })}
          />
        </label>
      ) : (
        <ProductFilters list={list} placeholder="Nome do produto" />
      )}
      <div
        className="party-picker-results"
        role="region"
        aria-label="Produtos encontrados"
        aria-busy={Boolean(list.loading)}
        tabIndex={0}
      >
        <ProductLoad state={list}>
          <div className="operation-choices">
            {list.data?.content.map((catalog) => (
              <button
                type="button"
                className={"secondary" + (orderId ? " operation-price-choice" : "")}
                key={catalog.id}
                disabled={Boolean(orderId) && !catalog.active}
                onClick={() => onSelect(catalog)}
              >
                <span>
                  {catalog.name} · {catalog.type === "SIMPLE" ? "Simples" : "Variações"}
                </span>
                {orderId && <CatalogSalePrice catalog={catalog} orderId={orderId} />}
              </button>
            ))}
          </div>
        </ProductLoad>
      </div>
      {compact ? (
        <div className="customer-list-footer">
          <span>
            {list.data ? `${list.data.totalElements} ${list.data.totalElements === 1 ? "produto encontrado" : "produtos encontrados"}` : ""}
          </span>
          <Pagination page={list.page} totalPages={list.data?.totalPages} change={list.changePage} />
        </div>
      ) : (
        <ProductPageFooter list={list} />
      )}
    </div>
  );
}
function SkuResults({ catalog, onSelect, orderId }) {
  const [page, setPage] = useState(0);
  const selected = useRef(false);
  const state = useProductResource(
    (options) => products.items(catalog.id, { active: true }, page, 25, options),
    [catalog.id, page],
  );
  useEffect(() => {
    if (
      catalog.type === "SIMPLE" &&
      state.data?.totalElements === 1 &&
      state.data.content.length === 1 &&
      !selected.current
    ) {
      selected.current = true;
      onSelect(state.data.content[0]);
    }
  }, [catalog.type, state.data, onSelect]);
  return (
    <div className="party-picker-content">
      <h3>{catalog.name}</h3>
      <div
        className="party-picker-results"
        role="region"
        aria-label="SKUs disponíveis"
        aria-busy={Boolean(state.loading)}
        tabIndex={0}
      >
        <ProductLoad state={state}>
          <div className="operation-choices">
            {state.data?.content.map((item) => (
              <button
                type="button"
                className={"secondary" + (orderId ? " operation-price-choice" : "")}
                key={item.id}
                onClick={() => onSelect(item)}
              >
                <span>
                  {skuName(item)} · {item.unit || "Unidade pendente"}
                </span>
                {orderId && <SkuSalePrice item={item} orderId={orderId} />}
              </button>
            ))}
          </div>
        </ProductLoad>
      </div>
      <div className="customer-list-footer">
        <span>
          {state.data
            ? `${state.data.totalElements.toLocaleString("pt-BR")} ${state.data.totalElements === 1 ? "SKU" : "SKUs"}`
            : ""}
        </span>
        <Pagination page={page} totalPages={state.data?.totalPages} change={setPage} />
      </div>
    </div>
  );
}

function CatalogSalePrice({ catalog, orderId }) {
  const state = useProductResource(
    async (options) => {
      const page = await products.items(catalog.id, { active: true }, 0, 1, options);
      const item = page.content[0];
      if (!item) throw new Error("Nenhum SKU ativo disponível.");
      return { amount: await orders.price(orderId, item.id, undefined, options), unit: item.unit };
    },
    [catalog.id, orderId],
    catalog.type === "SIMPLE",
  );
  if (catalog.type !== "SIMPLE")
    return <span className="operation-choice-price">Ver preços por variação</span>;
  return (
    <span
      className="operation-choice-price"
      title={state.error || "Preço por unidade BASE para o cliente do pedido"}
    >
      {state.loading
        ? "Consultando preço…"
        : state.error
          ? "Preço indisponível"
          : state.data
            ? `${money(state.data.amount)} / ${state.data.unit || "BASE"}`
            : "—"}
    </span>
  );
}

function SkuSalePrice({ item, orderId }) {
  const state = useProductResource(
    (options) => orders.price(orderId, item.id, undefined, options),
    [orderId, item.id],
  );
  return (
    <span
      className="operation-choice-price"
      title={state.error || "Preço por unidade BASE para o cliente do pedido"}
    >
      {state.loading
        ? "Consultando preço…"
        : state.error
          ? "Preço indisponível"
          : `${money(state.data)} / ${item.unit || "BASE"}`}
    </span>
  );
}

export function WarehouseSelect({ value, onChange, disabled = false, allowedIds }) {
  const { has } = useAuth();
  const allowed = has("warehouses.read");
  const state = useProductResource((options) => products.warehouses(options), [], allowed);
  const entries = state.data || [];
  const selectedExists = entries.some((warehouse) => String(warehouse.id) === String(value));
  return (
    <div>
      <label>
        Depósito
        <FilterSelect
          value={value}
          required
          disabled={disabled || !allowed || state.loading || Boolean(state.error)}
          onChange={onChange}
        >
          <option value="">{state.loading ? "Carregando depósitos…" : "Selecionar depósito"}</option>
          {value && !selectedExists && (
            <option value={value} disabled>
              Depósito #{value}
            </option>
          )}
          {entries.map((warehouse) => (
            <option
              key={warehouse.id}
              value={warehouse.id}
              disabled={allowedIds && !allowedIds.includes(warehouse.id)}
            >
              {warehouse.name}
            </option>
          ))}
        </FilterSelect>
      </label>
      {!allowed && <p>É necessária permissão de consulta de depósitos.</p>}
      {state.error && <ProductLoad state={state}>{null}</ProductLoad>}
    </div>
  );
}
