import { useEffect, useState } from "react";
import { Trash2 } from "lucide-react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RowActions from "../../components/feedback/RowActions";
import { products } from "../../services/products";
import { supplierCatalogLinks } from "../../services/supplierCatalogLinks";
import { useProductResource } from "../Products/useProductResource";
import useProductAction from "../Products/useProductAction";
import { ProductLoad } from "../Products/ProductControls";
import "../Products/products.css";

function CatalogLinks({ supplierId, kind, onBusy }) {
  const { has } = useAuth();
  const isBrand = kind === "brands";
  const field = isBrand ? "brand" : "category";
  const [selected, setSelected] = useState("");
  const [query, setQuery] = useState("");
  const state = useProductResource(
    (options) => supplierCatalogLinks[kind](supplierId, options),
    [supplierId, kind],
    has("suppliers.read"),
  );
  const available = useProductResource(
    (options) => products[kind]({ name: query }, options),
    [kind, query],
    has("products.read") && has("suppliers.update"),
  );
  const action = useProductAction();
  useEffect(() => {
    onBusy(action.pending);
    return () => onBusy(false);
  }, [action.pending, onBusy]);
  const add = () =>
    action.run(async () => {
      if (!has("suppliers.update") || !has("products.read"))
        throw new Error("Sem permissão para incluir a associação.");
      if (!selected) throw new Error("Selecione um cadastro disponível.");
      await (isBrand ? supplierCatalogLinks.registerBrand : supplierCatalogLinks.registerCategory)(
        supplierId,
        Number(selected),
      );
      setSelected("");
      state.reload();
    });
  const remove = (entry) =>
    action.run(
      async () => {
        if (!has("suppliers.update")) throw new Error("Sem permissão para remover a associação.");
        await (isBrand ? supplierCatalogLinks.deleteBrand : supplierCatalogLinks.deleteCategory)(
          supplierId,
          entry[field].id,
        );
        state.reload();
      },
      `Remover o vínculo com ${entry[field].name}? O cadastro de ${isBrand ? "marca" : "categoria"} e os produtos serão preservados.`,
      { title: "Remover vínculo", confirmLabel: "Remover vínculo" },
    );
  const options = (available.data || []).filter(
    (entry) => !state.data?.some((link) => link[field].id === entry.id),
  );
  return (
    <section>
      <h3>{isBrand ? "Marcas vinculadas" : "Categorias vinculadas"}</h3>
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <ProductLoad state={state} empty="Nenhum vínculo cadastrado.">
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
                <tr key={entry[field].id}>
                  <td>{entry[field].name}</td>
                  <td>
                    {has("suppliers.update") && (
                      <RowActions label={`Ações do vínculo ${entry[field].name}`}>
                        <button
                          title="Remover vínculo"
                          aria-label={`Remover vínculo com ${entry[field].name}`}
                          className="secondary icon-button"
                          disabled={action.pending}
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
      {has("suppliers.update") &&
        (has("products.read") ? (
          <>
            <ErrorAlert message={available.error} retry={available.reload} />
            <div className="product-toolbar">
              <label>
                Buscar cadastro
                <input
                  type="search"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    setSelected("");
                  }}
                />
              </label>
              <label>
                {isBrand ? "Marca" : "Categoria"}
                <FilterSelect
                  value={selected}
                  disabled={action.pending || !available.data || !state.data}
                  onChange={(e) => setSelected(e.target.value)}
                >
                  <option value="">Selecionar cadastro</option>
                  {options.map((entry) => (
                    <option key={entry.id} value={entry.id}>
                      {entry.name}
                    </option>
                  ))}
                </FilterSelect>
              </label>
              <button
                disabled={
                  action.pending ||
                  !selected ||
                  !state.data ||
                  !options.some((entry) => String(entry.id) === selected)
                }
                onClick={add}
              >
                Incluir vínculo
              </button>
            </div>
          </>
        ) : (
          <p className="muted">
            Para pesquisar marcas e categorias disponíveis, é necessária a permissão de consulta de
            Produtos (products.read). Os vínculos existentes continuam disponíveis conforme sua
            permissão de Fornecedores.
          </p>
        ))}
    </section>
  );
}

export default function SupplierCatalogLinksModal({ supplierId, onClose }) {
  const [kind, setKind] = useState("brands");
  const [busy, setBusy] = useState(false);
  return (
    <Modal title="Marcas e categorias do fornecedor" onClose={onClose} busy={busy} wide>
      <p className="muted">
        Vínculos explícitos e independentes dos produtos fornecidos. Remover uma associação preserva
        os cadastros.
      </p>
      <div className="product-toolbar" role="group" aria-label="Tipo de vínculo">
        <button
          disabled={busy}
          className={kind === "brands" ? "" : "secondary"}
          onClick={() => setKind("brands")}
        >
          Marcas
        </button>
        <button
          disabled={busy}
          className={kind === "categories" ? "" : "secondary"}
          onClick={() => setKind("categories")}
        >
          Categorias
        </button>
      </div>
      <CatalogLinks
        key={`${supplierId}:${kind}`}
        supplierId={supplierId}
        kind={kind}
        onBusy={setBusy}
      />
    </Modal>
  );
}
