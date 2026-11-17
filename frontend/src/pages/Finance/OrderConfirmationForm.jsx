import { useState } from "react";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useProductAction from "../Products/useProductAction";
import { orders } from "../../services/operations";
import FinanceSelect from "./FinanceSelect";
import { localDate,paymentLabels } from "./financeLabels";
export default function OrderConfirmationForm({ order,onClose,onSaved }) {
  const [method,setMethod]=useState(order.paymentMethod||"");
  const [due,setDue]=useState(order.paymentDueDate||"");
  const [attempt,setAttempt]=useState(null);
  const action=useProductAction();
  const future=["CREDIT_CARD","BOLETO"].includes(method);
  return <Modal title="Confirmar venda e reservar estoque" onClose={onClose} busy={action.pending}>
    <form className="finance-form" onSubmit={e=>{e.preventDefault();const body=attempt||{paymentMethod:method,...(future?{paymentDueDate:due}:{})};
      action.run(async()=>{setAttempt(body);await orders.confirm(order.id,body);onSaved();},"Confirmar a venda, criar uma conta a receber e reservar os itens?",{confirmLabel:"Confirmar venda"});}}>
      <ErrorAlert message={action.error}/>
      {attempt&&action.error&&<p role="alert">Confira o resultado no pedido ou repita os mesmos dados. A confirmação não cria outra conta.</p>}
      <fieldset disabled={action.pending||Boolean(attempt)}>
        <FinanceSelect required label="Forma de pagamento" empty="Selecione" value={method} values={paymentLabels} onChange={e=>setMethod(e.target.value)}/>
        {future&&<label>Vencimento futuro<input type="date" required min={localDate(1)} value={due} onChange={e=>setDue(e.target.value)}/></label>}
      </fieldset>
      <p>{future?"Informe o vencimento para crédito ou boleto demonstrativo.":"O vencimento será o dia da confirmação."} A baixa será manual e integral.</p>
      <div className="actions"><button type="button" className="secondary" disabled={action.pending} onClick={onClose}>Voltar ao pedido</button><button disabled={action.pending}>{action.pending?"Confirmando…":attempt?"Repetir os mesmos dados":"Confirmar venda"}</button></div>
    </form>
  </Modal>;
}
