import { useCallback, useState } from "react";
import { Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import RowActions from "../../components/feedback/RowActions";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import { useCatalogPage } from "./useProductResource";
import useProductAction from "./useProductAction";
import { ProductFilters, ProductLoad, ProductPageFooter } from "./ProductControls";
import { money, quantity, presentationPrice } from "./productFormData";
import ProductPresentationForm from "./ProductPresentationForm";

export default function ProductPresentations({ catalog, item }) {
  const { has } = useAuth();
  const read = useCallback(
    (filter, page, size, options) => products.presentations(item.id, filter, page, size, options),
    [item.id],
  );
  const list = useCatalogPage(read);
  const action = useProductAction();
  const [flow, setFlow] = useState(null);
  const toggle = (presentation) =>
    action.run(
      async () => {
        await products.updatePresentation(presentation.id, { active: !presentation.active });
        list.reload();
      },
      `${presentation.active ? "Inativar" : "Ativar"} a apresentação ${presentation.name}? O histórico e as condições já contratadas serão preservados.`,
      {
        title: "Situação da apresentação",
        confirmLabel: presentation.active ? "Inativar" : "Ativar",
      },
    );
  return (
    <section>
      <div className="product-toolbar">
        <h3>Apresentações de venda</h3>
        {has("products.create") && (
          <button
            className="customer-icon-label"
            disabled={!catalog.unit || action.pending}
            onClick={() => setFlow({})}
          >
            <Plus size={17} />
            Nova apresentação
          </button>
        )}
      </div>
      <p className="muted">
        Preço padrão do catálogo; a tabela do cliente pode definir outro preço na venda. Todas as
        apresentações usam o mesmo saldo BASE, sem contagem de embalagens lacradas.
      </p>
      {!item.active && (
        <p className="product-warning">
          SKU indisponível: apresentações não podem ser usadas em novos compromissos.
        </p>
      )}
      <ProductFilters list={list} placeholder="Nome da apresentação" />
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <ProductLoad state={list}>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Apresentação</th>
                <th>Equivalência BASE</th>
                <th>Preço padrão</th>
                <th>Situação</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.data?.content.map((presentation) => (
                <tr key={presentation.id}>
                  <td>
                    <strong>{presentation.name}</strong>
                    {presentation.basePresentation && (
                      <small>Base fixa · controlada pelo produto</small>
                    )}
                  </td>
                  <td>
                    {quantity(presentation.baseFactor)} {catalog.unit}
                  </td>
                  <td>
                    {presentation.price == null
                      ? presentationPrice(item.price, presentation.baseFactor)
                      : money(presentation.price)}
                    <small>
                      {presentation.price == null ? "Cálculo pelo preço base" : "Preço próprio"}
                    </small>
                  </td>
                  <td>
                    {presentation.active ? "Ativa" : "Inativa"}
                    {!item.active && <small>Indisponível pelo produto</small>}
                  </td>
                  <td>
                    {!presentation.basePresentation && has("products.update") && (
                      <RowActions label={`Ações de ${presentation.name}`}>
                        <button
                          title="Editar"
                          aria-label={`Editar ${presentation.name}`}
                          className="secondary icon-button"
                          disabled={action.pending || !catalog.unit}
                          onClick={() => setFlow({ presentation })}
                        >
                          <Pencil size={17} />
                        </button>
                        <button
                          title={presentation.active ? "Inativar" : "Ativar"}
                          aria-label={`${presentation.active ? "Inativar" : "Ativar"} ${presentation.name}`}
                          className="secondary icon-button row-status-action"
                          disabled={action.pending || !catalog.unit}
                          onClick={() => toggle(presentation)}
                        >
                          <Power size={17} />
                        </button>
                      </RowActions>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ProductLoad>
      <ProductPageFooter list={list} />
      {flow && (
        <ProductPresentationForm
          catalog={catalog}
          item={item}
          presentation={flow.presentation}
          onClose={() => setFlow(null)}
          onSaved={() => {
            setFlow(null);
            list.reload();
            action.succeed("Apresentação salva.");
          }}
        />
      )}
    </section>
  );
}
