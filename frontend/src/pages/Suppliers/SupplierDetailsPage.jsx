import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { suppliers } from "../../services/suppliers";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import SupplierDetailsContent, { supplierSections } from "./SupplierDetailsContent";
import "./supplierDetails.css";
import "../Products/products.css";

export default function SupplierDetailsPage() {
  const { id } = useParams();
  if (!validId(id)) return <p role="alert">Identificador de fornecedor inválido.</p>;
  return <Details key={id} id={Number(id)} />;
}
function Details({ id }) {
  const { has } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const section = supplierSections.includes(params.get("section"))
    ? params.get("section")
    : "general";
  const state = useProductResource((options) => suppliers.findOne(id, options), [id]);
  const { returnToList } = useListReturn({ modulePath: "/suppliers" });
  return (
    <div className="operation-page suppliers-page">
      <header className="page-title">
        <h1>Ficha do fornecedor</h1>
        <div className="actions">
          <button className="secondary" onClick={returnToList}>
            Voltar aos fornecedores
          </button>
          {has("suppliers.update") && state.data && (
            <button
              onClick={() =>
                navigate(`/suppliers/${id}/edit`, {
                  state: { ...location.state, partySection: section },
                })
              }
            >
              Editar fornecedor
            </button>
          )}
        </div>
      </header>
      <ProductLoad state={state} empty="Cadastro não encontrado.">
        {state.data && (
          <SupplierDetailsContent
            supplier={state.data}
            section={section}
            onSectionChange={(value) =>
              setParams({ section: value }, { replace: true, state: location.state })
            }
          />
        )}
      </ProductLoad>
    </div>
  );
}
