import { useId, useState } from "react";
import { Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import {
  units,
  characteristics,
  itemValues,
  itemPayload,
  variantIdentity,
} from "./productFormData";
import { useProductResource } from "./useProductResource";
import useProductForm from "./useProductForm";
import ProductItemFields from "./ProductItemFields";
import ProductClassificationsModal from "./ProductClassificationsModal";

export default function ProductForm({ catalog, item, onSaved, onClose }) {
  const classificationId = useId();
  const { has } = useAuth();
  const canWrite = has(catalog ? "products.update" : "products.create");
  const form = useProductForm(
    `catalog:${catalog?.id || "new"}`,
    {
      name: catalog?.name || "",
      brandId: String(catalog?.brand?.id || ""),
      categoryId: String(catalog?.category?.id || ""),
      unit: catalog?.unit || "",
      type: catalog?.type || "SIMPLE",
      characteristics: [...(catalog?.characteristics || [])],
      confirmUnit: false,
      items: [itemValues(item)],
    },
    onClose,
    onSaved,
  );
  const { value, change } = form;
  const [classification, setClassification] = useState(null);
  const options = useProductResource(async (request) => {
    const [brands, categories] = await Promise.all([
      products.brands({}, request),
      products.categories({}, request),
    ]);
    return { brands, categories };
  }, []);
  const itemChange = (index, next) =>
    change(
      "items",
      value.items.map((entry, at) => (at === index ? next : entry)),
    );
  const write = async () => {
    if (!canWrite) throw new Error("Sem permissão para salvar produtos.");
    if (catalog && !form.dirty) return;
    if (!value.name.trim() || !value.brandId || !value.unit)
      throw new Error("Informe nome, marca e unidade base.");
    if (catalog && value.unit !== catalog.unit && !value.confirmUnit)
      throw new Error("Confirme expressamente a unidade representada pelos saldos existentes.");
    const shared = {};
    for (const field of ["name", "unit"]) {
      if (!catalog || value[field] !== (catalog[field] || "")) shared[field] = value[field].trim();
    }
    for (const field of ["brand", "category"]) {
      const next = value[`${field}Id`];
      if (!catalog || next !== String(catalog[field]?.id || "")) {
        if (next) shared[`${field}Id`] = Number(next);
        else if (catalog && field === "category") shared.removeCategory = true;
      }
    }
    if (catalog) {
      // SIMPLE uses the existing atomic SKU endpoint for shared + commercial changes.
      if (catalog.type === "SIMPLE") {
        if (!item)
          throw new Error(
            "Não foi possível identificar o SKU do produto simples. Reabra os detalhes.",
          );
        const body = { ...shared, ...itemPayload(value.items[0], catalog, item, false) };
        if (!Object.keys(body).length) return;
        await products.updateItem(item.id, body);
        return catalog;
      }
      if (!Object.keys(shared).length) return;
      return products.update(catalog.id, shared);
    }
    if (value.type === "VARIANT" && !value.characteristics.length)
      throw new Error("Selecione ao menos uma característica.");
    const current = {
      ...value,
      characteristics: value.type === "SIMPLE" ? [] : value.characteristics,
    };
    const items = (value.type === "SIMPLE" ? value.items.slice(0, 1) : value.items).map((entry) =>
      itemPayload(entry, current, null, has("stock.configure") && has("warehouses.read")),
    );
    const identities = items.map((entry) => variantIdentity(entry.attributes || {}));
    if (!items.length || new Set(identities).size !== identities.length)
      throw new Error("Cadastre ao menos uma combinação e não repita variações.");
    return products.register({
      ...shared,
      type: current.type,
      characteristics: current.characteristics,
      items,
    });
  };
  return (
    <>
      <Modal
        title={catalog ? "Editar produto" : "Novo produto"}
        onClose={form.close}
        busy={form.saving}
        wide
      >
        <ErrorAlert message={form.error} />
        <ErrorAlert message={options.error} retry={options.reload} />
        <form onSubmit={(event) => form.submit(event, write)} className="customer-form">
          <fieldset className="customer-form-fields" disabled={form.saving || !canWrite}>
            <div className="form-grid">
              <label className="span-all">
                Tipo de produto
                <FilterSelect
                  disabled={!!catalog}
                  value={value.type}
                  onChange={(e) => change("type", e.target.value)}
                >
                  <option value="SIMPLE">Simples</option>
                  <option value="VARIANT">Com variações</option>
                </FilterSelect>
              </label>
              <label className="span-all">
                Nome
                <input
                  required
                  value={value.name}
                  onChange={(e) => change("name", e.target.value)}
                />
              </label>
              <div className="product-classification-field">
                <label htmlFor={`${classificationId}-brand`}>Marca</label>
                <span
                  className={`product-classification-select${has("products.create") ? " has-add" : ""}`}
                >
                  {has("products.create") && (
                    <button
                      type="button"
                      className="product-classification-add"
                      aria-label="Adicionar marca"
                      title="Adicionar marca"
                      onClick={() => setClassification("brands")}
                    >
                      <Plus size={17} aria-hidden="true" />
                    </button>
                  )}
                  <FilterSelect
                    id={`${classificationId}-brand`}
                    aria-label="Marca"
                    required
                    disabled={!options.data}
                    value={value.brandId}
                    onChange={(e) => change("brandId", e.target.value)}
                  >
                    <option value="">Selecionar marca</option>
                    {catalog?.brand &&
                      !options.data?.brands.some((brand) => brand.id === catalog.brand.id) && (
                        <option value={catalog.brand.id}>{catalog.brand.name}</option>
                      )}
                    {(options.data?.brands || []).map((brand) => (
                      <option key={brand.id} value={brand.id}>
                        {brand.name}
                      </option>
                    ))}
                  </FilterSelect>
                </span>
              </div>
              <div className="product-classification-field">
                <label htmlFor={`${classificationId}-category`}>Categoria (opcional)</label>
                <span
                  className={`product-classification-select${has("products.create") ? " has-add" : ""}`}
                >
                  {has("products.create") && (
                    <button
                      type="button"
                      className="product-classification-add"
                      aria-label="Adicionar categoria"
                      title="Adicionar categoria"
                      onClick={() => setClassification("categories")}
                    >
                      <Plus size={17} aria-hidden="true" />
                    </button>
                  )}
                  <FilterSelect
                    id={`${classificationId}-category`}
                    aria-label="Categoria (opcional)"
                    disabled={!options.data}
                    value={value.categoryId}
                    onChange={(e) => change("categoryId", e.target.value)}
                  >
                    <option value="">Sem categoria</option>
                    {catalog?.category &&
                      !options.data?.categories.some(
                        (category) => category.id === catalog.category.id,
                      ) && <option value={catalog.category.id}>{catalog.category.name}</option>}
                    {(options.data?.categories || []).map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </FilterSelect>
                </span>
              </div>
              <label>
                Unidade base
                <FilterSelect
                  required={!catalog}
                  disabled={!!catalog?.unit}
                  value={value.unit}
                  onChange={(e) => change("unit", e.target.value)}
                >
                  <option value="">Selecionar unidade</option>
                  {Object.entries(units).map(([code, label]) => (
                    <option key={code} value={code}>
                      {code} · {label}
                    </option>
                  ))}
                </FilterSelect>
              </label>
              {catalog && (
                <p className="muted span-all">
                  Tipo, características e unidade confirmada ficam fixos nesta tela. Alterações com
                  histórico exigem tratamento próprio; não é possível reagrupar itens.
                </p>
              )}
              {catalog && !catalog.unit && (
                <div className="product-warning span-all">
                  <p>
                    Unidade legada pendente. Escolher uma unidade identifica os números existentes;
                    não converte saldos. Se os números representam caixas e o destino são unidades,
                    é necessária uma conversão de dados própria. As opções de fornecedor exigem
                    conferência independente.
                  </p>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={value.confirmUnit}
                      onChange={(e) => change("confirmUnit", e.target.checked)}
                    />
                    Confirmo que a unidade escolhida representa os saldos e quantidades existentes,
                    sem conversão.
                  </label>
                </div>
              )}
              {value.type === "VARIANT" && (
                <fieldset className="span-all product-characteristics" disabled={!!catalog}>
                  <legend>Características comuns</legend>
                  {Object.entries(characteristics).map(([code, label]) => (
                    <label key={code} className="check">
                      <input
                        type="checkbox"
                        checked={value.characteristics.includes(code)}
                        onChange={(e) =>
                          change(
                            "characteristics",
                            e.target.checked
                              ? [...value.characteristics, code]
                              : value.characteristics.filter((field) => field !== code),
                          )
                        }
                      />
                      {label}
                    </label>
                  ))}
                </fieldset>
              )}
            </div>
            {(!catalog || catalog.type === "SIMPLE") &&
              (value.type === "SIMPLE" ? value.items.slice(0, 1) : value.items).map(
                (entry, index) => (
                  <section key={index} className="product-item-editor">
                    {value.type === "VARIANT" && (
                      <div className="product-toolbar">
                        <h3>Combinação {index + 1}</h3>
                        {value.items.length > 1 && (
                          <button
                            type="button"
                            className="secondary"
                            onClick={() =>
                              change(
                                "items",
                                value.items.filter((_, at) => at !== index),
                              )
                            }
                          >
                            Remover combinação não salva
                          </button>
                        )}
                      </div>
                    )}
                    <ProductItemFields
                      value={entry}
                      onChange={(next) => itemChange(index, next)}
                      catalog={value}
                      item={item}
                      disabled={form.saving}
                    />
                  </section>
                ),
              )}
            {!catalog && value.type === "VARIANT" && (
              <button
                type="button"
                className="secondary product-add-combination"
                onClick={() => change("items", [...value.items, itemValues(null)])}
              >
                Adicionar combinação
              </button>
            )}
          </fieldset>
          <div className="actions">
            <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
              Cancelar
            </button>
            <button disabled={form.saving || !canWrite || (!catalog && !options.data)}>
              {form.saving ? "Salvando…" : "Salvar produto"}
            </button>
          </div>
        </form>
      </Modal>
      {classification && (
        <ProductClassificationsModal
          kind={classification}
          onClose={() => {
            setClassification(null);
            options.reload();
          }}
          onChanged={options.reload}
        />
      )}
    </>
  );
}
