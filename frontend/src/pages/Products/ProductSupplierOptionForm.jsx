import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { productSuppliers } from "../../services/productSuppliers";
import { baseNumber, decimal, textNumber } from "./productFormData";
import useProductForm from "./useProductForm";
import ProductNumberInput from "./ProductNumberInput";

export default function ProductSupplierOptionForm({
  catalog,
  item,
  link,
  option,
  onSaved,
  onClose,
}) {
  const { has } = useAuth();
  const pendingLegacy = option && !option.conversionConfirmed;
  const form = useProductForm(
    `supplier-option:${item.id}:${link.id}:${option?.id || "new"}`,
    {
      name: option?.name || "",
      baseFactor: textNumber(option?.baseFactor),
      basePresentation: option?.basePresentation == null ? "" : String(option.basePresentation),
      cost: textNumber(option?.cost),
      avgDeliveryDays: textNumber(option?.avgDeliveryDays),
      supplierProductCode: option?.supplierProductCode || "",
      confirmConversion: false,
    },
    onClose,
    onSaved,
  );
  const write = async () => {
    if (!has("suppliers.update"))
      throw new Error("Sem permissão para alterar opções do fornecedor.");
    if (option && !form.dirty) return;
    if (!form.value.name.trim() || form.value.basePresentation === "")
      throw new Error("Informe nome e tipo da opção.");
    if (pendingLegacy && !form.value.confirmConversion)
      throw new Error(
        "Confira e confirme expressamente unidade, equivalência e custo da opção legada.",
      );
    const next = {
      name: form.value.name.trim(),
      baseFactor: baseNumber(form.value.baseFactor, catalog.unit, "Equivalência BASE", false, true),
      basePresentation: form.value.basePresentation === "true",
      cost: decimal(form.value.cost, "Custo da embalagem"),
      avgDeliveryDays: decimal(form.value.avgDeliveryDays, "Prazo específico", {
        optional: true,
        places: 0,
        max: 2147483647,
      }),
      supplierProductCode: form.value.supplierProductCode.trim(),
    };
    if (next.basePresentation && next.baseFactor !== 1)
      throw new Error("A opção de unidade base deve ter equivalência 1.");
    if (!option) return productSuppliers.registerOption({ ...next, productSupplierId: link.id });
    const body = {};
    for (const field of Object.keys(next)) {
      const previous =
        field === "supplierProductCode" ? option[field] || "" : (option[field] ?? null);
      if (next[field] !== previous) {
        if (field === "avgDeliveryDays" && next[field] == null) body.removeAvgDeliveryDays = true;
        else body[field] = next[field];
      }
    }
    if (pendingLegacy)
      Object.assign(body, {
        confirmConversion: true,
        baseFactor: next.baseFactor,
        basePresentation: next.basePresentation,
        cost: next.cost,
      });
    if (!Object.keys(body).length) return;
    return productSuppliers.updateOption(option.id, body);
  };
  return (
    <Modal
      title={option ? "Editar opção de compra" : "Nova opção de compra"}
      onClose={form.close}
      busy={form.saving}
      wide
    >
      <p>
        {link.supplier.legalName} · {item.sku}
      </p>
      <ErrorAlert message={form.error} />
      <form className="customer-form" onSubmit={(e) => form.submit(e, write)}>
        <fieldset
          className="customer-form-fields"
          disabled={form.saving || !has("suppliers.update")}
        >
          <div className="form-grid">
            <label>
              Nome
              <input
                required
                value={form.value.name}
                onChange={(e) => form.change("name", e.target.value)}
              />
            </label>
            <label>
              Tipo
              <FilterSelect
                required
                value={form.value.basePresentation}
                onChange={(e) => form.change("basePresentation", e.target.value)}
              >
                <option value="">Selecionar tipo</option>
                <option value="true">Unidade base (fator 1)</option>
                <option value="false">Embalagem composta</option>
              </FilterSelect>
            </label>
            <label>
              Quantidade {catalog.unit || "BASE"} por embalagem
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
              Custo da embalagem
              <ProductNumberInput
                label="Custo da embalagem"
                required
                inputMode="decimal"
                value={form.value.cost}
                onChange={(e) => form.change("cost", e.target.value)}
              />
            </label>
            <label>
              Código no fornecedor (opcional)
              <input
                value={form.value.supplierProductCode}
                onChange={(e) => form.change("supplierProductCode", e.target.value)}
              />
            </label>
            <label>
              Prazo específico em dias (opcional)
              <ProductNumberInput
                label="Prazo específico"
                places={0}
                max={2147483647}
                inputMode="numeric"
                value={form.value.avgDeliveryDays}
                onChange={(e) => form.change("avgDeliveryDays", e.target.value)}
              />
              <small>Vazio usa o prazo geral do fornecedor. Zero é prazo informado.</small>
            </label>
          </div>
          {pendingLegacy && (
            <div className="product-warning">
              <p>
                Conversão legada pendente. Confira unidade, equivalência e custo. Confirmar não
                reinterpreta recebimentos anteriores nem converte saldos.
              </p>
              <label className="check">
                <input
                  type="checkbox"
                  checked={form.value.confirmConversion}
                  onChange={(e) => form.change("confirmConversion", e.target.checked)}
                />
                Conferi a unidade {catalog.unit || "pendente"}, o fator e o custo desta opção.
              </label>
            </div>
          )}
          <p className="muted">
            Custo de referência e preço de venda são independentes. Uma equivalência já usada exige
            inativar esta opção e cadastrar outra.
          </p>
          {!catalog.unit && (
            <p className="product-warning">
              Confirme primeiro a unidade do catálogo; isso não confirma esta opção automaticamente.
            </p>
          )}
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
            Cancelar
          </button>
          <button disabled={form.saving || !has("suppliers.update") || !catalog.unit}>
            {form.saving ? "Salvando…" : "Salvar opção"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
