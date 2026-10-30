import { useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import RecordMetadata from "../../components/record/RecordMetadata";
import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import { ProductLoad } from "./ProductControls";
import ProductForm from "./ProductForm";
import ProductVariants from "./ProductVariants";
import { ProductItemContent } from "./ProductItemDetails";

export default function ProductDetailsModal({
  catalogId,
  query,
  onClose,
  onChanged,
  startEdit = false,
  startStock = false,
}) {
  const { has } = useAuth();
  const [editing, setEditing] = useState(startEdit);
  const [notice, setNotice] = useState("");
  const state = useProductResource(
    async (options) => {
      const catalog = await products.findOne(catalogId, options);
      const page =
        catalog.type === "SIMPLE" ? await products.items(catalogId, {}, 0, 25, options) : null;
      if (catalog.type === "SIMPLE" && page?.content.length !== 1)
        throw new Error("O produto simples não possui um único SKU disponível. Atualize os dados.");
      return { catalog, item: page?.content[0] };
    },
    [catalogId],
  );
  const saved = () => {
    setEditing(false);
    state.reload();
    onChanged();
    setNotice("Produto salvo.");
  };
  const catalog = state.data?.catalog;
  return (
    <>
      <Modal title="Detalhes do produto" onClose={onClose} wide>
        {notice && (
          <p role="status" className="success">
            {notice}
          </p>
        )}
        <ProductLoad state={state}>
          {catalog && (
            <>
              <div className="customer-detail-title">
                <h3>{catalog.name}</h3>
                <span className={`badge ${catalog.active ? "active" : ""}`}>
                  {catalog.active ? "Ativo" : "Inativo"}
                </span>
              </div>
              <dl className="customer-details">
                {[
                  ["Marca", catalog.brand?.name],
                  ["Categoria", catalog.category?.name],
                  ["Unidade base", catalog.unit || "Pendente de conferência"],
                  ["Tipo", catalog.type === "SIMPLE" ? "Simples" : "Com variações"],
                ].map(([label, value]) => (
                  <div key={label}>
                    <dt>{label}</dt>
                    <dd>{value || "—"}</dd>
                  </div>
                ))}
              </dl>
              {has("products.update") && (
                <div className="actions">
                  <button onClick={() => setEditing(true)}>Editar produto</button>
                </div>
              )}
              <RecordMetadata record={catalog} />
              <p className="muted">
                As datas acima pertencem ao cadastro principal. Dados do SKU possuem registro
                próprio; alterações de estoque não são representadas por essas datas.
              </p>
              {catalog.type === "VARIANT" ? (
                <ProductVariants
                  key={catalog.id}
                  catalog={catalog}
                  query={query}
                  startStock={startStock}
                  onChanged={onChanged}
                />
              ) : (
                state.data.item && (
                  <ProductItemContent
                    key={state.data.item.id}
                    catalog={catalog}
                    item={state.data.item}
                    initialTab={startStock ? "stock" : "commercial"}
                    onStockChanged={onChanged}
                  />
                )
              )}
            </>
          )}
        </ProductLoad>
      </Modal>
      {editing && catalog && has("products.update") && (
        <ProductForm
          catalog={catalog}
          item={state.data.item}
          onSaved={saved}
          onClose={() => {
            setEditing(false);
            if (startEdit) onClose();
          }}
        />
      )}
    </>
  );
}
