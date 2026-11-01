import { useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import RecordMetadata from "../../components/record/RecordMetadata";
import { characteristics, money } from "./productFormData";
import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import { ProductLoad } from "./ProductControls";
import ProductPresentations from "./ProductPresentations";
import ProductSuppliers from "./ProductSuppliers";
import ProductStockPanel from "./ProductStockPanel";
import ProductStockDialog from "./ProductStockDialog";

export function ProductItemContent({ catalog, item, initialTab = "commercial", onStockChanged }) {
  const { has } = useAuth();
  const [tab, setTab] = useState(initialTab);
  const [stockOpen, setStockOpen] = useState(false);
  const measure = (value, places) =>
    value == null ? "—" : Number(value).toLocaleString("pt-BR", { maximumFractionDigits: places });
  return (
    <>
      <div className="product-toolbar product-tabs" role="group" aria-label="Dados do SKU">
        {has("stock.read") && (
          <button type="button" className="secondary" onClick={() => setStockOpen(true)}>
            Estoque
          </button>
        )}
        <button
          className={tab === "commercial" ? "" : "secondary"}
          onClick={() => setTab("commercial")}
        >
          Dados comerciais
        </button>
        <button
          className={tab === "presentations" ? "" : "secondary"}
          onClick={() => setTab("presentations")}
        >
          Apresentações de venda
        </button>
        {has("suppliers.read") && (
          <button
            className={tab === "suppliers" ? "" : "secondary"}
            onClick={() => setTab("suppliers")}
          >
            Fornecedores / opções
          </button>
        )}
      </div>
      {!catalog.unit && (
        <p className="product-warning">
          Unidade legada pendente de conferência. Novas operações exigem confirmar o significado dos
          saldos, sem conversão automática.
        </p>
      )}
      {tab === "commercial" && (
        <>
          <dl className="customer-details">
            {[
              ["SKU", item.sku],
              ["Custo de referência", money(item.cost)],
              ["Preço base", money(item.price)],
              ["Unidade base", catalog.unit || "Pendente"],
              ["Peso (kg)", measure(item.weightKg, 3)],
              ["Dimensão X", measure(item.dimensionX, 2)],
              ["Dimensão Y", measure(item.dimensionY, 2)],
              ["Dimensão Z", measure(item.dimensionZ, 2)],
              ...Object.entries(item.attributes || {}).map(([code, value]) => [
                characteristics[code],
                value,
              ]),
              ...(catalog.type === "SIMPLE"
                ? [
                    ["Cor", item.color],
                    ["Tamanho", item.size],
                  ]
                : []),
            ].map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value ?? "—"}</dd>
              </div>
            ))}
          </dl>
          <RecordMetadata record={item} />
        </>
      )}
      {tab === "presentations" && (
        <ProductPresentations key={item.id} catalog={catalog} item={item} />
      )}
      {tab === "suppliers" && has("suppliers.read") && (
        <ProductSuppliers key={item.id} catalog={catalog} item={item} />
      )}
      {stockOpen && has("stock.read") && (
        <ProductStockDialog
          item={item}
          onChanged={onStockChanged}
          onClose={() => setStockOpen(false)}
        />
      )}
    </>
  );
}
export default function ProductItemDetails({
  catalog,
  item,
  onClose,
  onEdit,
  notice,
  initialTab,
  onStockChanged,
}) {
  const { has } = useAuth();
  const state = useProductResource((options) => products.findItem(item.id, options), [item.id]);
  if (initialTab === "stock" && has("stock.read")) {
    return (
      <Modal title={`Estoque · ${item.sku}`} onClose={onClose} wide className="product-stock-dialog">
        <ProductLoad state={state} empty="Variação não encontrada.">
          {state.data && <ProductStockPanel item={state.data} onChanged={onStockChanged} />}
        </ProductLoad>
      </Modal>
    );
  }
  return (
    <Modal title={`Variação ${item.sku}`} onClose={onClose} wide className="product-details-dialog">
      <h3>{catalog.name}</h3>
      {notice && (
        <p role="status" className="success">
          {notice}
        </p>
      )}
      <ProductLoad state={state} empty="Variação não encontrada.">
        {state.data && (
          <>
            <p>
              {state.data.ownActive ? "Ativa própria" : "Inativa própria"}
              {!catalog.active ? " · Indisponível pelo principal" : ""}
            </p>
            <ProductItemContent
              catalog={catalog}
              item={state.data}
              initialTab={initialTab}
              onStockChanged={onStockChanged}
            />
            {onEdit && has("products.update") && (
              <div className="actions">
                <button type="button" onClick={() => onEdit(state.data)}>
                  Editar variação
                </button>
              </div>
            )}
          </>
        )}
      </ProductLoad>
    </Modal>
  );
}
