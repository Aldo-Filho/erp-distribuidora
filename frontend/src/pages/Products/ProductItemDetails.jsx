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

export function ProductItemContent({ catalog, item, initialTab = "commercial", onStockChanged }) {
  const { has } = useAuth();
  const [tab, setTab] = useState(initialTab);
  const measure = (value, places) =>
    value == null ? "—" : Number(value).toLocaleString("pt-BR", { maximumFractionDigits: places });
  return (
    <>
      <div className="product-toolbar product-tabs" role="group" aria-label="Dados do SKU">
        {has("stock.read") && (
          <button type="button" className={tab === "stock" ? "" : "secondary"}
            onClick={() => setTab("stock")}>Estoque</button>
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
      {tab === "stock" && has("stock.read") && (
        <ProductStockPanel key={item.id} item={item} onChanged={onStockChanged} />
      )}
    </>
  );
}
export default function ProductItemDetails({ catalog, item, onClose, initialTab, onStockChanged }) {
  const state = useProductResource((options) => products.findItem(item.id, options), [item.id]);
  return (
    <Modal title={`Variação ${item.sku}`} onClose={onClose} wide>
      <h3>{catalog.name}</h3>
      <ProductLoad state={state}>
        {state.data && (
          <>
            <p>
              {state.data.ownActive ? "Ativa própria" : "Inativa própria"}
              {!catalog.active ? " · Indisponível pelo principal" : ""}
            </p>
            <ProductItemContent catalog={catalog} item={state.data}
              initialTab={initialTab} onStockChanged={onStockChanged} />
          </>
        )}
      </ProductLoad>
    </Modal>
  );
}
