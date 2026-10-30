import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import { itemValues, itemPayload } from "./productFormData";
import { useProductResource } from "./useProductResource";
import { ProductLoad } from "./ProductControls";
import ProductItemFields from "./ProductItemFields";
import useProductForm from "./useProductForm";

export default function ProductVariantForm({ catalog, item, onSaved, onClose }) {
  const state = useProductResource(
    (options) => products.findItem(item.id, options),
    [item?.id],
    !!item,
  );
  if (!item) return <VariantForm catalog={catalog} onSaved={onSaved} onClose={onClose} />;
  if (state.data)
    return (
      <VariantForm
        key={state.data.id}
        catalog={catalog}
        item={state.data}
        onSaved={onSaved}
        onClose={onClose}
      />
    );
  return (
    <Modal title="Editar variação" onClose={onClose}>
      <ProductLoad state={state} />
    </Modal>
  );
}

function VariantForm({ catalog, item, onSaved, onClose }) {
  const { has } = useAuth();
  const canWrite = has(item ? "products.update" : "products.create");
  const form = useProductForm(
    `variant:${catalog.id}:${item?.id || "new"}`,
    itemValues(item),
    onClose,
    onSaved,
  );
  const write = async () => {
    if (!canWrite) throw new Error("Sem permissão para salvar variações.");
    const body = itemPayload(
      form.value,
      catalog,
      item,
      has("stock.configure") && has("warehouses.read"),
    );
    if (item && !Object.keys(body).length) return;
    return item ? products.updateItem(item.id, body) : products.registerItem(catalog.id, body);
  };
  return (
    <Modal
      title={item ? "Editar variação" : "Nova variação"}
      onClose={form.close}
      busy={form.saving}
      wide
    >
      <p>
        {catalog.name} · Unidade base: {catalog.unit || "pendente"}
      </p>
      <ErrorAlert message={form.error} />
      <form className="customer-form" onSubmit={(e) => form.submit(e, write)}>
        <fieldset className="customer-form-fields" disabled={form.saving || !canWrite}>
          <ProductItemFields
            catalog={catalog}
            item={item}
            value={form.value}
            onChange={form.setValue}
            disabled={form.saving}
          />
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            Cancelar
          </button>
          <button disabled={form.saving || !canWrite}>
            {form.saving ? "Salvando…" : "Salvar variação"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
