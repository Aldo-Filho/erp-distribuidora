import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import { baseNumber, decimal, textNumber } from "./productFormData";
import useProductForm from "./useProductForm";
import ProductNumberInput from "./ProductNumberInput";

export default function ProductPresentationForm({ catalog, item, presentation, onClose, onSaved }) {
  const { has } = useAuth();
  const canWrite = has(presentation ? "products.update" : "products.create");
  const form = useProductForm(
    `presentation:${item.id}:${presentation?.id || "new"}`,
    {
      name: presentation?.name || "",
      baseFactor: textNumber(presentation?.baseFactor),
      price: textNumber(presentation?.price),
    },
    onClose,
    onSaved,
  );
  const write = async () => {
    if (!canWrite || presentation?.basePresentation)
      throw new Error("Sem permissão para alterar esta apresentação.");
    if (!form.value.name.trim()) throw new Error("Informe o nome da apresentação.");
    const next = {
      name: form.value.name.trim(),
      baseFactor: baseNumber(form.value.baseFactor, catalog.unit, "Equivalência BASE", false, true),
      price: decimal(form.value.price, "Preço próprio", { optional: true }),
    };
    if (!presentation) return products.registerPresentation({ ...next, productId: item.id });
    const body = {};
    for (const field of ["name", "baseFactor", "price"]) {
      if (next[field] !== (presentation[field] ?? null)) {
        if (field === "price" && next.price == null) body.removePrice = true;
        else body[field] = next[field];
      }
    }
    if (!Object.keys(body).length) return;
    return products.updatePresentation(presentation.id, body);
  };
  return (
    <Modal
      title={presentation ? "Editar apresentação de venda" : "Nova apresentação de venda"}
      onClose={form.close}
      busy={form.saving}
    >
      <p>
        {item.sku} · {catalog.name}
      </p>
      <ErrorAlert message={form.error} />
      <form onSubmit={(e) => form.submit(e, write)} className="customer-form">
        <fieldset className="customer-form-fields" disabled={form.saving || !canWrite}>
          <div className="form-grid">
            <label className="span-all">
              Nome
              <input
                required
                value={form.value.name}
                onChange={(e) => form.change("name", e.target.value)}
              />
            </label>
            <label>
              Quantidade {catalog.unit} por embalagem
              <ProductNumberInput
                label="Quantidade por embalagem"
                places={catalog.unit === "UN" ? 0 : 1}
                max={9999999999999.9}
                required
                inputMode="decimal"
                value={form.value.baseFactor}
                onChange={(e) => form.change("baseFactor", e.target.value)}
              />
            </label>
            <label>
              Preço próprio (opcional)
              <ProductNumberInput
                label="Preço próprio"
                inputMode="decimal"
                value={form.value.price}
                onChange={(e) => form.change("price", e.target.value)}
              />
            </label>
          </div>
          <p className="muted">
            Sem preço próprio, usa o preço base multiplicado pela equivalência. Zero é preço
            informado. A tabela do cliente pode definir outro preço na venda.
          </p>
          <p className="muted">
            Equivalência já usada em operação não pode ser alterada. Nesse caso, inative a
            apresentação e cadastre outra. Todas as apresentações compartilham o estoque BASE.
          </p>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
            Cancelar
          </button>
          <button disabled={form.saving || !canWrite || !catalog.unit}>
            {form.saving ? "Salvando…" : "Salvar apresentação"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
