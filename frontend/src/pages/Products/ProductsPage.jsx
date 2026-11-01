import useListReturn from "../../hooks/useListReturn";
import { useState } from "react";
import { Eye, History, Package, Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import FilterSelect from "../../components/feedback/FilterSelect";
import RowActions from "../../components/feedback/RowActions";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import useProducts, { emptyFilters } from "./useProducts";
import { useProductResource } from "./useProductResource";
import useProductAction from "./useProductAction";
import { ProductLoad, ProductPageFooter } from "./ProductControls";
import { inactiveCatalogMessage } from "./productFormData";
import ProductClassificationsModal from "./ProductClassificationsModal";
import ProductListRow from "./ProductListRow";
import "./products.css";

export default function ProductsPage() {
  const { has } = useAuth();
  const origin = useListReturn({ modulePath: "/products" });
  const list = useProducts(origin.restored);
  const listReturn = useListReturn({
    modulePath: "/products",
    listState: { filters: list.filters, page: list.page, size: list.size },
    ready: Boolean(list.data),
  });
  const action = useProductAction();
  const [flow, setFlow] = useState(null);
  const classifications = useProductResource(async (options) => {
    const [brands, categories] = await Promise.all([
      products.brands({}, options),
      products.categories({}, options),
    ]);
    return { brands, categories };
  }, []);
  const filter = (field, next) => list.changeFilters({ ...list.filters, [field]: next });
  const filtered = Object.values(list.filters).some((value) => String(value).trim());
  const toggle = (catalog) => {
    if (!has("products.update")) return;
    const impact = catalog.active
      ? inactiveCatalogMessage
      : "Somente as variações com situação própria ativa voltarão a ficar disponíveis. Variações previamente inativas continuarão inativas.";
    return action.run(
      async () => {
        await products.update(catalog.id, { active: !catalog.active });
        list.reload();
      },
      `${catalog.active ? "Inativar" : "Ativar"} o produto ${catalog.name}? ${impact}`,
      {
        title: catalog.active ? "Inativar produto" : "Ativar produto",
        confirmLabel: catalog.active ? "Inativar" : "Ativar",
        destructive: catalog.active,
        successMessage: catalog.active ? "Produto inativado." : "Produto ativado.",
      },
    );
  };
  return (
    <div className="customers-page products-page">
      <div className="page-title">
        <PageHeading
          title="Produtos"
          total={list.data?.totalElements}
          loading={list.loading}
          singular={filtered ? "produto encontrado" : "produto cadastrado"}
          plural={filtered ? "produtos encontrados" : "produtos cadastrados"}
        />
        <div className="product-toolbar">
          {has("stock.read") && (
            <button
              type="button"
              className="secondary customer-icon-label"
              disabled={action.pending}
              onClick={() => listReturn.open("/stock/history")}
            >
              <History size={17} />
              Histórico de estoque
            </button>
          )}
          <button
            className="secondary"
            disabled={action.pending}
            onClick={() => setFlow({ kind: "brands" })}
          >
            Marcas
          </button>
          <button
            className="secondary"
            disabled={action.pending}
            onClick={() => setFlow({ kind: "categories" })}
          >
            Categorias
          </button>
          {has("products.create") && (
            <button
              className="customer-icon-label"
              disabled={action.pending}
              onClick={() => listReturn.open("/products/new")}
            >
              <Plus size={17} />
              Novo produto
            </button>
          )}
        </div>
      </div>
      <section className="panel customer-filters" aria-label="Filtros de produtos">
        <label className="customer-query">
          Busca
          <input
            type="search"
            placeholder="Nome, SKU ou características"
            value={list.filters.query}
            onChange={(e) => filter("query", e.target.value)}
          />
        </label>
        {[
          ["brandId", "Marca", classifications.data?.brands],
          ["categoryId", "Categoria", classifications.data?.categories],
        ].map(([field, label, entries]) => (
          <label key={field}>
            {label}
            <FilterSelect
              value={list.filters[field]}
              disabled={!entries}
              onChange={(e) => filter(field, e.target.value)}
            >
              <option value="">Todas</option>
              {(entries || []).map((entry) => (
                <option key={entry.id} value={entry.id}>
                  {entry.name}
                </option>
              ))}
            </FilterSelect>
          </label>
        ))}
        <label>
          Situação
          <FilterSelect
            value={list.filters.active}
            onChange={(e) => filter("active", e.target.value)}
          >
            <option value="">Todas</option>
            <option value="true">Ativos</option>
            <option value="false">Inativos</option>
          </FilterSelect>
        </label>
        <button className="secondary" onClick={() => list.changeFilters({ ...emptyFilters })}>
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={classifications.error} retry={classifications.reload} />
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <section
        className="panel table-panel"
        aria-label="Lista de produtos"
        aria-busy={list.loading}
      >
        <ProductLoad
          state={list}
          empty={
            filtered
              ? "Nenhum produto encontrado para estes filtros."
              : "Nenhum produto cadastrado."
          }
        >
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Produto</th>
                  <th>Marca</th>
                  <th>Categoria</th>
                  <th>Situação</th>
                  <th>Preço</th>
                  {has("stock.read") && <th>Estoque</th>}
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {list.data?.content.map((catalog) => (
                  <ProductListRow
                    key={catalog.id}
                    catalog={catalog}
                    canReadStock={has("stock.read")}
                    onDetails={() => listReturn.open(`/products/${catalog.id}`)}
                    onStock={() => listReturn.open(`/products/${catalog.id}?section=stock`)}
                  >
                    <td>
                      <RowActions label={`Ações de ${catalog.name}`}>
                        <button
                          title="Detalhes"
                          aria-label={`Detalhes de ${catalog.name}`}
                          className="secondary icon-button"
                          disabled={action.pending}
                          onClick={() => listReturn.open(`/products/${catalog.id}`)}
                        >
                          <Eye size={17} />
                        </button>
                        {has("products.update") && (
                          <button
                            title="Editar"
                            aria-label={`Editar ${catalog.name}`}
                            className="secondary icon-button"
                            disabled={action.pending}
                            onClick={() => listReturn.open(`/products/${catalog.id}/edit`)}
                          >
                            <Pencil size={17} />
                          </button>
                        )}
                        {has("stock.read") && (
                          <button
                            type="button"
                            title="Estoque"
                            aria-label={`Estoque de ${catalog.name}`}
                            className="secondary icon-button"
                            disabled={action.pending}
                            onClick={() => listReturn.open(`/products/${catalog.id}?section=stock`)}
                          >
                            <Package size={17} />
                          </button>
                        )}
                        {has("products.update") && (
                          <button
                            title={catalog.active ? "Inativar" : "Ativar"}
                            aria-label={`${catalog.active ? "Inativar" : "Ativar"} ${catalog.name}`}
                            className="secondary icon-button row-status-action"
                            disabled={action.pending}
                            onClick={() => toggle(catalog)}
                          >
                            <Power size={17} />
                          </button>
                        )}
                      </RowActions>
                    </td>
                  </ProductListRow>
                ))}
              </tbody>
            </table>
          </div>
        </ProductLoad>
        <ProductPageFooter list={list} />
      </section>
      {["brands", "categories"].includes(flow?.kind) && (
        <ProductClassificationsModal
          kind={flow.kind}
          onClose={() => {
            setFlow(null);
            classifications.reload();
          }}
          onChanged={classifications.reload}
        />
      )}
    </div>
  );
}
