import { useRef } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import useListReturn from "../../hooks/useListReturn";
import { validId } from "../../routes/navigation";
import PurchaseDetailsContent from "./PurchaseDetailsContent";

export default function PurchaseDetailsPage() {
  const { id } = useParams();
  const leaveRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();
  const { returnToList } = useListReturn({ modulePath: "/purchases" });
  if (!validId(id)) return <p role="alert">Identificador de compra inválido.</p>;
  return (
    <div className="purchases-page operation-page">
      <header className="page-title">
        <h1>Compra #{id}</h1>
        <button
          className="secondary"
          onClick={() => (leaveRef.current ? leaveRef.current() : returnToList())}
        >
          Voltar às compras
        </button>
      </header>
      <PurchaseDetailsContent
        key={id}
        id={Number(id)}
        leaveRef={leaveRef}
        embedded
        onClose={returnToList}
        onEdit={() => navigate(`/purchases/${id}/edit`, { state: location.state })}
      />
    </div>
  );
}
