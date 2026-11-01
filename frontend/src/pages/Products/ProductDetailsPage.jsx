import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { ProductLoad } from "./ProductControls";
import useProductPage from "./useProductPage";
import ProductDetailsContent, { productSections } from "./ProductDetailsContent";
import "./products.css";

export default function ProductDetailsPage() {
  const { id } = useParams();
  if (!validId(id)) return <p role="alert">Identificador de produto inválido.</p>;
  return <Details key={id} id={Number(id)} />;
}
function Details({ id }) {
  const { has } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const section = productSections.includes(params.get("section"))
    ? params.get("section")
    : "general";
  const state = useProductPage(id);
  const { returnToList } = useListReturn({ modulePath: "/products" });
  return (
    <div className="products-page operation-page">
      <header className="page-title">
        <h1>Ficha do produto</h1>
        <div className="actions">
          <button className="secondary" onClick={returnToList}>
            Voltar aos produtos
          </button>
          {has("products.update") && state.data && (
            <button
              onClick={() =>
                navigate(`/products/${id}/edit`, {
                  state: { ...location.state, productSection: section },
                })
              }
            >
              Editar produto
            </button>
          )}
        </div>
      </header>
      <ProductLoad state={state} empty="Produto não encontrado.">
        {state.data && (
          <ProductDetailsContent
            {...state.data}
            section={section}
            onSectionChange={(value) =>
              setParams({ section: value }, { replace: true, state: location.state })
            }
            onChanged={state.reload}
          />
        )}
      </ProductLoad>
    </div>
  );
}
