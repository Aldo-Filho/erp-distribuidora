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
import ProductStockPanel from "./ProductStockPanel";

export default function ProductDetailsModal({
  catalogId,
  query,
  onClose,
  onChanged,
  onSaved,
  startEdit = false,
  startStock = false,
}) {
  const { has } = useAuth();
  const [editing, setEditing] = useState(startEdit);
  const [notice, setNotice] = useState("");
  const state = useProductResource(
    async (options) => {
      const catalog = await products.findOne(catalogId, options);
      if (!catalog) return null;
      const page =
        catalog.type === "SIMPLE" ? await products.items(catalogId, {}, 0, 25, options) : null;
      if (catalog.type === "SIMPLE" && page?.content.length !== 1)
        throw new Error("O produto simples não possui um único SKU disponível. Atualize os dados.");
      return { catalog, item: page?.content[0] };
    },
    [catalogId],
  );
  const saved = () => {
    onChanged();
    if (startEdit) {
      onSaved?.();
      onClose();
      return;
    }
    setEditing(false);
    state.reload();
    setNotice("Produto salvo.");
  };
  const catalog = state.data?.catalog;
  if (startStock && has("stock.read")) {
    return (
      <Modal title="Estoque do produto" onClose={onClose} wide className="product-stock-dialog">
        <ProductLoad state={state} empty="Produto não encontrado.">
          {catalog && (
            <>
              <h3>{catalog.name}</h3>
              {catalog.type === "VARIANT" ? (
                <ProductVariants
                  catalog={catalog}
                  query={query}
                  startStock
                  onChanged={onChanged}
                />
              ) : (
                <ProductStockPanel item={state.data.item} onChanged={onChanged} />
              )}
            </>
          )}
        </ProductLoad>
      </Modal>
    );
  }
  if (editing && catalog && has("products.update")) {
    return (
      <ProductForm
        catalog={catalog}
        item={state.data.item}
        onSaved={saved}
        onClose={() => {
          setEditing(false);
          if (startEdit) onClose();
        }}
      />
    );
  }
  return (
    <Modal
      title={editing ? "Editar produto" : "Detalhes do produto"}
      onClose={onClose}
      wide
      className="product-details-dialog"
    >
      {notice && (
        <p role="status" className="success">
          {notice}
        </p>
      )}
      <ProductLoad state={state} empty="Produto não encontrado.">
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
  );
}
