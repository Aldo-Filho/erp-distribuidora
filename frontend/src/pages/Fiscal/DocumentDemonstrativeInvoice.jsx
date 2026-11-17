import { useState } from "react";
import { FileText, Printer } from "lucide-react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { useProductResource } from "../Products/useProductResource";
import { orders } from "../../services/operations";
import DemonstrativeInvoiceDocument from "./DemonstrativeInvoiceDocument";
import { printDemonstrativeInvoice } from "./printDemonstrativeInvoice";
import "./demonstrativeInvoice.css";

export default function DocumentDemonstrativeInvoice({id,status}) {
  const {session,has}=useAuth();
  if(!has("orders.read")||status!=="INVOICED")return null;
  return <Invoice key={`${session.userId}:${id}`} id={id}/>;
}
function Invoice({id}) {
  const [open,setOpen]=useState(false),[printError,setPrintError]=useState("");
  const state=useProductResource(async options=>{
    try{return await orders.demonstrativeInvoice(id,options);}
    catch(issue){if(issue.status===404)return null;throw issue;}
  },[id],open);
  function print() {
    setPrintError("");
    try{printDemonstrativeInvoice(state.data);}catch(issue){setPrintError(issue.message);}
  }
  return <section className="panel demo-invoice-link"><h3>NF-e demonstrativa</h3>
    <p className="field-hint">Documento preservado no despacho · sem validade fiscal.</p>
    <button type="button" className="secondary customer-icon-label" onClick={()=>setOpen(true)}><FileText size={17}/>Consultar documento demonstrativo</button>
    {open&&<Modal title="NF-e demonstrativa — sem validade fiscal" wide onClose={()=>setOpen(false)}>
      {state.loading?<p role="status">Consultando documento…</p>:state.error?<ErrorAlert message={state.error} retry={state.reload}/>:state.data?<>
        <ErrorAlert message={printError}/><div className="demo-invoice-actions"><button type="button" className="customer-icon-label" onClick={print}><Printer size={17}/>Imprimir demonstrativo</button></div>
        <DemonstrativeInvoiceDocument document={state.data}/>
      </>:<div><p>Nenhum documento demonstrativo armazenado para esta venda.</p><p className="field-hint">Vendas despachadas antes desta funcionalidade não recebem emissão retroativa. A consulta não cria nem altera documentos.</p></div>}
    </Modal>}
  </section>;
}
