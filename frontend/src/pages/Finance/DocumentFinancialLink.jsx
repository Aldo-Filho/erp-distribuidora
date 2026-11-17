import { Link } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import { useProductResource } from "../Products/useProductResource";
import { receivables, payables } from "../../services/finance";
import { money } from "../Products/productFormData";
import { statusLabels } from "./financeLabels";
export default function DocumentFinancialLink({ id, purchase=false, status }) {
  const { has }=useAuth();
  const allowed=has("finance.read");
  const state=useProductResource(options=>(purchase?payables:receivables).search({documentId:id,size:1},options),[id,purchase,status],allowed);
  if(!allowed) return null;
  const title=state.data?.content[0];
  return <section className="panel"><h3>Financeiro</h3>
    {state.loading?<p>Consultando conta financeira…</p>:state.error?<p role="alert">Não foi possível consultar a conta. <button className="secondary" onClick={state.reload}>Tentar novamente</button></p>:title?<p>Conta #{title.id} · {statusLabels[title.status]} · {money(title.amount)}</p>:<p>Nenhuma conta vinculada. A conta é criada na confirmação.</p>}
    <Link to={`/finance/${purchase?"payables":"receivables"}?documentId=${id}`}>Consultar conta, baixas e estornos</Link>
  </section>;
}
