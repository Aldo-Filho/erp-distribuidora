import { useRef } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import { suppliers } from "../../services/suppliers";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import SupplierForm from "./SupplierForm";
import { supplierSections } from "./SupplierDetailsContent";

export default function SupplierEntryPage() {
  const { id } = useParams();
  const { has } = useAuth();
  if (id && !validId(id)) return <p role="alert">Identificador inválido.</p>;
  if (!has(id ? "suppliers.update" : "suppliers.create"))
    return <p>Sem permissão para esta operação.</p>;
  return <Entry key={id || "new"} id={id ? Number(id) : null} />;
}
function Entry({ id }) {
  const closeRef = useRef(null);
  const state = useProductResource((options) => suppliers.findOne(id, options), [id], Boolean(id));
  const navigate = useNavigate();
  const location = useLocation();
  const { returnToList } = useListReturn({ modulePath: "/suppliers" });
  const back = () =>
    id
      ? navigate(
          `/suppliers/${id}?section=${supplierSections.includes(location.state?.partySection) ? location.state.partySection : "general"}`,
          { state: location.state },
        )
      : returnToList();
  const form = (
    <section className="panel">
      <SupplierForm
        closeRef={closeRef}
        embedded
        supplier={state.data}
        onClose={back}
        onSaved={(saved) => {
          if (id) {
            back();
            return;
          }
          const savedId = saved?.id;
          if (validId(savedId))
            navigate(`/suppliers/${savedId}`, { replace: true, state: location.state });
          else returnToList();
        }}
      />
    </section>
  );
  return (
    <div className="operation-page suppliers-page">
      <header className="page-title">
        <h1>{id ? "Editar fornecedor" : "Novo fornecedor"}</h1>
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
        <ProductLoad state={state} empty="Cadastro não encontrado.">
          {state.data && form}
        </ProductLoad>
      ) : (
        form
      )}
    </div>
  );
}
