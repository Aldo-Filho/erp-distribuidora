import { useRef } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { ProductLoad } from "./ProductControls";
import ProductForm from "./ProductForm";
import useProductPage from "./useProductPage";
import "./products.css";

export default function ProductEntryPage() {
  const { id } = useParams();
  const { has } = useAuth();
  if (id && !validId(id)) return <p role="alert">Identificador de produto inválido.</p>;
  if (!has(id ? "products.update" : "products.create"))
    return <p>Sem permissão para esta operação.</p>;
  return <Entry key={id || "new"} id={id ? Number(id) : null} />;
}
function Entry({ id }) {
  const closeRef = useRef(null);
  const state = useProductPage(id);
  const navigate = useNavigate();
  const location = useLocation();
  const { returnToList } = useListReturn({ modulePath: "/products" });
  const back = () =>
    id
      ? navigate(
          `/products/${id}?section=${["general", "commercial", "variants", "presentations", "suppliers", "stock"].includes(location.state?.productSection) ? location.state.productSection : "general"}`,
          { state: location.state },
        )
      : returnToList();
  const form = (
    <section className="panel">
      <ProductForm
        closeRef={closeRef}
        embedded
        {...state.data}
        onClose={back}
        onSaved={(saved) => {
          if (id) back();
          else if (validId(saved?.id))
            navigate(`/products/${saved.id}`, { replace: true, state: location.state });
          else returnToList();
        }}
      />
    </section>
  );
  return (
    <div className="products-page operation-page">
      <header className="page-title">
        <h1>{id ? "Editar produto" : "Novo produto"}</h1>
        <button
          type="button"
          className="secondary"
          disabled={Boolean(id && !state.data)}
          onClick={() => closeRef.current?.()}
        >
          {id ? "Voltar à ficha" : "Voltar à lista"}
        </button>
      </header>
      {id ? (
        <ProductLoad state={state} empty="Produto não encontrado.">
          {state.data && form}
        </ProductLoad>
      ) : (
        form
      )}
    </div>
  );
}
