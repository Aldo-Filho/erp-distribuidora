import { useState } from "react";
import { Trash2 } from "lucide-react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RowActions from "../../components/feedback/RowActions";
import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import useProductForm from "./useProductForm";
import useProductAction from "./useProductAction";
import { ProductLoad } from "./ProductControls";

export default function ProductClassificationsModal({ kind, onClose, onChanged = () => {} }) {
  const { has } = useAuth();
  const isBrand = kind === "brands";
  const [query, setQuery] = useState("");
  const state = useProductResource(
    (options) => products[kind]({ name: query }, options),
    [kind, query],
    has("products.read"),
  );
  const action = useProductAction();
  const form = useProductForm(`classification:${kind}:new`, { name: "" }, onClose, () => {
    form.change("name", "");
    state.reload();
    onChanged();
    action.succeed(isBrand ? "Marca cadastrada." : "Categoria cadastrada.");
  });
  const write = () => {
    if (!has("products.create")) throw new Error("Sem permissão para cadastrar.");
    if (!form.value.name.trim()) throw new Error("Informe o nome.");
    return (isBrand ? products.registerBrand : products.registerCategory)({
      name: form.value.name.trim(),
    });
  };
  const remove = (entry) =>
    action.run(
      async () => {
        try {
          await (isBrand ? products.deleteBrand : products.deleteCategory)(entry.id);
        } catch (issue) {
          if (issue.status === 409)
            throw new Error(
              `Não foi possível excluir ${entry.name}: há referências ou vínculos. Remova as associações permitidas antes de tentar novamente.`,
            );
          throw issue;
        }
        state.reload();
        onChanged();
      },
      `Excluir ${isBrand ? "a marca" : "a categoria"} ${entry.name}? A exclusão só é permitida sem referências e não remove produtos ou fornecedores.`,
      {
        title: isBrand ? "Excluir marca" : "Excluir categoria",
        confirmLabel: "Excluir",
        destructive: true,
      },
    );
  return (
    <Modal
      title={isBrand ? "Marcas" : "Categorias"}
      onClose={action.pending ? () => {} : form.close}
      busy={form.saving || action.pending}
    >
      <ErrorAlert message={form.error} />
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      {has("products.create") && (
        <form className="customer-form" onSubmit={(e) => form.submit(e, write)}>
          <fieldset className="customer-form-fields" disabled={form.saving || action.pending}>
            <label>
              {isBrand ? "Nova marca" : "Nova categoria"}
              <input
                required
                value={form.value.name}
                onChange={(e) => form.change("name", e.target.value)}
              />
            </label>
          </fieldset>
          <div className="actions">
            <button disabled={form.saving || action.pending}>
              {form.saving ? "Salvando…" : "Cadastrar"}
            </button>
          </div>
        </form>
      )}
      <label>
        Buscar por nome
        <input type="search" value={query} onChange={(e) => setQuery(e.target.value)} />
      </label>
      <ProductLoad state={state}>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Nome</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {state.data?.map((entry) => (
                <tr key={entry.id}>
                  <td>{entry.name}</td>
                  <td>
                    {has("products.delete") && (
                      <RowActions label={`Ações de ${entry.name}`}>
                        <button
                          title="Excluir"
                          aria-label={`Excluir ${entry.name}`}
                          className="secondary icon-button danger-link"
                          disabled={form.saving || action.pending}
                          onClick={() => remove(entry)}
                        >
                          <Trash2 size={17} />
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
    </Modal>
  );
}
