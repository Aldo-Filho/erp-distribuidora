import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { customers } from "../../services/customers";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import CustomerDetailsContent, { customerSections } from "./CustomerDetailsContent";
import "./customerDetails.css";
import "../Products/products.css";

export default function CustomerDetailsPage() {
  const { id } = useParams();
  if (!validId(id)) return <p role="alert">Identificador de cliente inválido.</p>;
  return <Details key={id} id={Number(id)} />;
}
function Details({ id }) {
  const { has } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const section = customerSections.includes(params.get("section"))
    ? params.get("section")
    : "general";
  const state = useProductResource((options) => customers.findOne(id, options), [id]);
  const { returnToList } = useListReturn({ modulePath: "/customers" });
  return (
    <div className="operation-page customers-page">
      <header className="page-title">
        <h1>Ficha do cliente</h1>
        <div className="actions">
          <button className="secondary" onClick={returnToList}>
            Voltar aos clientes
          </button>
          {has("customers.update") && state.data && (
            <button
              onClick={() =>
                navigate(`/customers/${id}/edit`, {
                  state: { ...location.state, partySection: section },
                })
              }
            >
              Editar cliente
            </button>
          )}
        </div>
      </header>
      <ProductLoad state={state} empty="Cadastro não encontrado.">
        {state.data && (
          <CustomerDetailsContent
            customer={state.data}
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
