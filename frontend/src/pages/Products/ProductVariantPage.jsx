import { useState } from "react";
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import { validId } from "../../routes/navigation";
import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import { ProductLoad } from "./ProductControls";
import { ProductItemContent } from "./ProductItemDetails";
import ProductVariantForm from "./ProductVariantForm";
import "./products.css";

export default function ProductVariantPage() {
  const { id, itemId } = useParams();
  if (!validId(id) || !validId(itemId)) return <p role="alert">Identificador inválido.</p>;
  return <Variant key={`${id}:${itemId}`} id={Number(id)} itemId={Number(itemId)} />;
}
function Variant({ id, itemId }) {
  const { has } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const [editing, setEditing] = useState(false);
  const section = params.get("section") || "commercial";
  const state = useProductResource(
    async (options) => {
      const catalog = await products.findOne(id, options);
      if (!catalog || catalog.type !== "VARIANT") return null;
      const item = await products.findItem(itemId, options);
      if (!item || Number(item.catalogId) !== id) return null;
      return { catalog, item };
    },
    [id, itemId],
  );
  return (
    <div className="products-page operation-page">
      <header className="page-title">
        <h1>Ficha da variação</h1>
        <button
          className="secondary"
          onClick={() => navigate(`/products/${id}?section=variants`, { state: location.state })}
        >
          Voltar ao produto
        </button>
      </header>
      <ProductLoad state={state} empty="Variação não encontrada neste catálogo.">
        {state.data && (
          <section className="panel">
            <h2>
              {state.data.catalog.name} · {state.data.item.sku}
            </h2>
            <p>
              {state.data.item.ownActive ? "Ativa própria" : "Inativa própria"}
              {!state.data.catalog.active && " · Indisponível pelo principal"}
            </p>
            <nav className="product-toolbar">
              {[
                "commercial",
                "presentations",
                ...(has("suppliers.read") ? ["suppliers"] : []),
                ...(has("stock.read") ? ["stock"] : []),
              ].map((key) => (
                <button
                  key={key}
                  className={section === key ? "" : "secondary"}
                  onClick={() =>
                    setParams({ section: key }, { replace: true, state: location.state })
                  }
                >
                  {
                    {
                      commercial: "Dados comerciais",
                      presentations: "Apresentações",
                      suppliers: "Fornecedores / opções",
                      stock: "Estoque",
                    }[key]
                  }
                </button>
              ))}
            </nav>
            {["commercial", "presentations", "suppliers", "stock"].includes(section) ? (
              <ProductItemContent
                {...state.data}
                section={section}
                onSectionChange={(value) =>
                  setParams({ section: value }, { replace: true, state: location.state })
                }
                onStockChanged={state.reload}
              />
            ) : (
              <p>Seção inválida.</p>
            )}
            {has("products.update") && (
              <button onClick={() => setEditing(true)}>Editar variação</button>
            )}
          </section>
        )}
      </ProductLoad>
      {editing && state.data && has("products.update") && (
        <ProductVariantForm
          {...state.data}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false);
            state.reload();
          }}
        />
      )}
    </div>
  );
}
