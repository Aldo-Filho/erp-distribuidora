import { useCallback, useState } from "react";
import { Eye, Package, Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import RowActions from "../../components/feedback/RowActions";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import { useCatalogPage } from "./useProductResource";
import useProductAction from "./useProductAction";
import { ProductFilters, ProductLoad, ProductPageFooter } from "./ProductControls";
import { characteristics, money } from "./productFormData";
import ProductVariantForm from "./ProductVariantForm";
import ProductItemDetails from "./ProductItemDetails";
import ProductStockCell from "./ProductStockCell";

export default function ProductVariants({ catalog, query = "", onChanged, startStock = false }) {
  const { has } = useAuth();
  const read = useCallback(
    (filter, page, size, options) => products.items(catalog.id, filter, page, size, options),
    [catalog.id],
  );
  const list = useCatalogPage(read, { query, active: "" });
  const action = useProductAction();
  const [flow, setFlow] = useState(null);
  const closeFlow = () =>
    setFlow((previous) =>
      previous?.returnTo ? { kind: "details", item: previous.item, stock: previous.stock } : null,
    );
  const saved = () => {
    closeFlow();
    list.reload();
    onChanged();
    action.succeed("Variação salva.");
  };
  const toggle = (item) => {
    if (!has("products.update") || (!item.ownActive && !catalog.active)) return;
    return action.run(
      async () => {
        await products.updateItem(item.id, { active: !item.ownActive });
        list.reload();
        onChanged();
      },
      item.ownActive
        ? `Inativar a variação ${item.sku}? Saldos e histórico serão preservados. Compromissos já confirmados poderão ser concluídos ou cancelados.`
        : `Ativar a variação ${item.sku}?`,
      {
        title: item.ownActive ? "Inativar variação" : "Ativar variação",
        confirmLabel: item.ownActive ? "Inativar" : "Ativar",
        destructive: item.ownActive,
        successMessage: item.ownActive ? "Variação inativada." : "Variação ativada.",
      },
    );
  };
  return (
    <section>
      {startStock && has("stock.read") && (
        <p className="field-hint">Selecione o saldo de uma variação para consultar e movimentar seu estoque.</p>
      )}
      <div className="product-toolbar">
        <h3>Variações</h3>
        {has("products.create") && (
          <button
            className="customer-icon-label"
            disabled={action.pending || !catalog.unit}
            onClick={() => setFlow({ kind: "new" })}
          >
            <Plus size={17} />
            Nova variação
          </button>
        )}
      </div>
      {query && list.filters.query === query && (
        <p className="muted">
          Busca original aplicada às variações: {query}. Limpe a busca para consultar todas.
        </p>
      )}
      {!catalog.active && (
        <p className="product-warning">
          Principal inativo: todas as variações estão indisponíveis para novas compras e vendas. As
          situações próprias foram preservadas.
        </p>
      )}
      <ProductFilters list={list} placeholder="SKU ou características" />
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <ProductLoad state={list} empty="Nenhuma variação corresponde aos filtros.">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>SKU / características</th>
                <th>Preço base</th>
                <th>Situação</th>
              {has("stock.read") && <th>Estoque</th>}
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.data?.content.map((item) => (
                <tr
                  key={item.id}
                  className={list.filters.query.trim() ? "product-match" : undefined}
                >
                  <td>
                    <strong>{item.sku}</strong>
                    <small>
                      {Object.entries(item.attributes || {})
                        .map(([code, value]) => `${characteristics[code]}: ${value}`)
                        .join(" · ")}
                    </small>
                  </td>
                  <td>{money(item.price)}</td>
                  <td>
                    {item.ownActive ? "Ativa própria" : "Inativa própria"}
                    {!catalog.active && <small>Indisponível pelo principal</small>}
                  </td>
                  {has("stock.read") && (
                    <td>
                      <ProductStockCell item={item} />
                    </td>
                  )}
                  <td>
                    <RowActions label={`Ações de ${item.sku}`}>
                      <button
                        className="secondary icon-button"
                        title="Detalhes"
                        aria-label={`Detalhes de ${item.sku}`}
                        disabled={action.pending}
                        onClick={() => setFlow({ kind: "details", item })}
                      >
                        <Eye size={17} />
                      </button>
                      {has("products.update") && (
                        <button
                          className="secondary icon-button"
                          title="Editar"
                          aria-label={`Editar ${item.sku}`}
                          disabled={action.pending}
                          onClick={() => setFlow({ kind: "edit", item })}
                        >
                          <Pencil size={17} />
                        </button>
                      )}
                      {has("stock.read") && (
                        <button
                          type="button"
                          title="Estoque"
                          aria-label={`Estoque de ${item.sku}`}
                          className="secondary icon-button"
                          disabled={action.pending}
                          onClick={() => setFlow({ kind: "details", item, stock: true })}
                        >
                          <Package size={17} />
                        </button>
                      )}
                      {has("products.update") && (
                        <button
                          className="secondary icon-button row-status-action"
                          title={item.ownActive ? "Inativar" : "Ativar"}
                          aria-label={`${item.ownActive ? "Inativar" : "Ativar"} ${item.sku}`}
                          disabled={action.pending || (!item.ownActive && !catalog.active)}
                          onClick={() => toggle(item)}
                        >
                          <Power size={17} />
                        </button>
                      )}
                    </RowActions>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ProductLoad>
      <ProductPageFooter list={list} />
      {((flow?.kind === "edit" && has("products.update")) ||
        (flow?.kind === "new" && has("products.create"))) && (
        <ProductVariantForm
          key={flow.item?.id || "new"}
          catalog={catalog}
          item={flow.item}
          onClose={closeFlow}
          onSaved={saved}
        />
      )}
      {flow?.kind === "details" && (
        <ProductItemDetails
          catalog={catalog}
          item={flow.item}
          initialTab={flow.stock ? "stock" : "commercial"}
          notice={action.notice}
          onStockChanged={() => {
            list.reload();
            onChanged();
          }}
          onEdit={(item) => setFlow({ kind: "edit", item, returnTo: true, stock: flow.stock })}
          onClose={closeFlow}
        />
      )}
    </section>
  );
}
