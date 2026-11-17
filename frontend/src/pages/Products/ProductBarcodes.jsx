import { useState } from "react";
import useAuth from "../../auth/useAuth";
import { barcodes } from "../../services/barcodes";
import { useProductResource } from "./useProductResource";
import useProductAction from "./useProductAction";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
export default function ProductBarcodes({productId}) {
 const {has}=useAuth();const [page,setPage]=useState(0);const [code,setCode]=useState("");const [type,setType]=useState("INTERNAL");
 const [mainCode,setMain]=useState(false);const action=useProductAction();
 const state=useProductResource(options=>barcodes.search(productId,page,options),[productId,page],has("products.read"));
 if(!has("products.read"))return null;
 return <section className="panel"><h3>Códigos de barras do SKU</h3><ErrorAlert message={state.error||action.error} retry={state.reload}/>
  {has("products.update")&&<div className="form-grid"><label>Código<input value={code} maxLength={128} disabled={action.pending} onChange={e=>setCode(e.target.value)}/></label>
  <label>Tipo<select value={type} disabled={action.pending} onChange={e=>setType(e.target.value)}><option value="INTERNAL">Interno</option><option value="EAN_GTIN">EAN / GTIN</option></select></label>
  <label><input type="checkbox" checked={mainCode} disabled={action.pending} onChange={e=>setMain(e.target.checked)}/>Principal</label>
  <button type="button" disabled={action.pending||!code.trim()} onClick={()=>action.run(async()=>{await barcodes.register({productId,code,type,mainCode});setCode("");state.reload();})}>Cadastrar código</button></div>}
  {state.loading?<p>Carregando códigos…</p>:<ul>{state.data?.content.map(b=><li key={b.id}><strong>{b.code}</strong> · {b.type} · {b.active?"Ativo":"Inativo"}{b.mainCode?" · Principal":""}
   {has("products.update")&&<><button type="button" className="secondary" disabled={action.pending} onClick={()=>action.run(async()=>{await barcodes.update(b.id,{active:!b.active});state.reload();})}>{b.active?"Inativar":"Ativar"}</button>
   {b.active&&!b.mainCode&&<button type="button" className="secondary" disabled={action.pending} onClick={()=>action.run(async()=>{await barcodes.update(b.id,{mainCode:true});state.reload();})}>Tornar principal</button>}</>}</li>)}</ul>}
   {!state.loading&&!state.error&&!state.data?.content.length&&<p>Nenhum código cadastrado.</p>}
   <Pagination page={page} totalPages={state.data?.totalPages||0} change={setPage}/>
 </section>;
}
