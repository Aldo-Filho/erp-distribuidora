import { useState } from "react";
import { Link } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import { useProductResource } from "../Products/useProductResource";
import useProductAction from "../Products/useProductAction";
import { money,quantity } from "../Products/productFormData";
import { purchaseSuggestions } from "../../services/purchaseSuggestions";
const results={GENERATED:"Rascunho gerado",COVERED:"Sem falta descoberta",NO_SUPPLIER:"Sem fornecedor elegível"};
export default function PurchaseSuggestionsPage(){
 const {has}=useAuth();const [page,setPage]=useState(0);const [selected,setSelected]=useState(null);
 const [requestId,setRequestId]=useState(null);const action=useProductAction();
 const state=useProductResource(options=>purchaseSuggestions.search(page,options),[page],has("purchases.read"));
 if(!has("purchases.read"))return <p>Sem permissão para consultar compras.</p>;
 return <div className="operation-page"><header className="page-title"><PageHeading title="Sugestões de compra" total={state.data?.totalElements} loading={state.loading} singular="geração" plural="gerações"/>
  {has("purchases.suggest")&&has("purchases.create")&&<button disabled={action.pending} onClick={()=>{
   const key=requestId||crypto.randomUUID();
   action.run(async()=>{setRequestId(key);const run=await purchaseSuggestions.generate(key);setSelected(run.purchase_suggestion_id);setRequestId(null);state.reload();},"Calcular reposição e criar rascunhos para revisão? Nenhuma compra será enviada ou confirmada.",{confirmLabel:"Gerar rascunhos"});
  }}>{action.pending?"Gerando…":requestId?"Conferir/repetir geração":"Gerar sugestões"}</button>}
 </header><p className="panel">Média diária = vendas despachadas nos últimos 90 dias ÷ 90. Estoque alvo = maior entre mínimo e média × (prazo + 7 dias). Falta = alvo − disponível − compras geradas em aberto. A quantidade é arredondada para cima conforme a apresentação do fornecedor. Fornecedor preferencial ativo tem prioridade; na ausência, usa-se o menor custo por unidade BASE.</p>
 <ErrorAlert message={state.error||action.error} retry={state.reload}/>
 {requestId&&action.error&&<p role="alert">O resultado pode ter sido registrado. Confira a lista ou repita com o mesmo identificador; não será criada uma segunda geração.</p>}
 {state.loading?<p>Carregando gerações…</p>:<section className="panel"><ul>{state.data?.content.map(run=><li key={run.purchase_suggestion_id}><button className="secondary" onClick={()=>setSelected(run.purchase_suggestion_id)}>Geração #{run.purchase_suggestion_id} · {String(run.generated_at).replace("T"," ")}</button></li>)}</ul>{!state.data?.content.length&&<p>Nenhuma geração encontrada.</p>}</section>}
 <Pagination page={page} totalPages={state.data?.totalPages||0} change={setPage}/>
 {selected&&<SuggestionItems key={selected} id={selected}/>}
 </div>;
}
function SuggestionItems({id}){
 const {has}=useAuth();const [page,setPage]=useState(0);
 const state=useProductResource(options=>purchaseSuggestions.items(id,page,options),[id,page]);
 return <section className="panel"><h2>Memória do cálculo — geração #{id}</h2><p>Os valores são snapshots da geração. Alterações posteriores no rascunho são feitas pela compra normal e entram na cobertura da próxima geração.</p>
 <ErrorAlert message={state.error} retry={state.reload}/>{state.loading?<p>Carregando itens…</p>:<div className="table-scroll"><table><thead><tr><th>SKU / depósito</th><th>Consumo em 90 dias</th><th>Média diária</th><th>Estoque</th><th>Alvo</th><th>Cobertura anterior</th><th>Falta / compra BASE</th><th>Fornecedor / opção</th><th>Resultado</th></tr></thead><tbody>{state.data?.content.map(i=><tr key={i.purchase_suggestion_item_id}>
 <td>{i.sku} · {i.product_name}<small>{i.warehouse_name||"Legado sem depósito"}</small></td><td>{quantity(i.period_sold_quantity)}</td><td>{Number(i.avg_daily_consumption||0).toLocaleString("pt-BR",{maximumFractionDigits:6})}</td>
 <td>Físico {quantity(i.current_stock)}<br/>Reservado {quantity(i.reserved_stock)}<br/>Disponível {quantity(i.available_stock)}<br/>Mínimo {quantity(i.min_stock)}</td>
 <td>{Number(i.target_stock||0).toLocaleString("pt-BR",{maximumFractionDigits:6})} · Prazo {i.lead_days??0} dias</td><td>{quantity(i.covered_quantity)}</td><td>Falta {Number(i.shortage_quantity||0).toLocaleString("pt-BR",{maximumFractionDigits:6})}<br/>Compra {quantity(i.suggested_quantity)}<small>{quantity(i.commercial_quantity)} apresentações × {quantity(i.base_factor)}</small></td>
 <td>{i.supplier_name||"Nenhum"}<br/>{i.selection_rule==="PREFERRED"?"Preferencial":i.selection_rule==="LOWEST_BASE_COST"?"Menor custo BASE":"Sem opção ativa"}<br/>{i.supplier_option_id?`Opção #${i.supplier_option_id}`:""}{i.unit_cost!=null&&<span> · {money(i.unit_cost)}</span>}</td>
 <td>{results[i.result]||"Histórico"}{i.purchase_id&&<><br/><Link to={`/purchases/${i.purchase_id}${i.draft_status==="DRAFT"&&has("purchases.update")?"/edit":""}`}>Compra #{i.purchase_id} · {i.draft_status}</Link></>}</td></tr>)}</tbody></table>{!state.data?.content.length&&<p>Nenhuma necessidade encontrada para esta geração.</p>}</div>}
 <Pagination page={page} totalPages={state.data?.totalPages||0} change={setPage}/></section>;
}
