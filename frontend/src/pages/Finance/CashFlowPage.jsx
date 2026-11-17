import { useSearchParams } from "react-router-dom";
import { useState } from "react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import { useProductResource } from "../Products/useProductResource";
import { money } from "../Products/productFormData";
import { cashFlow, bankAccounts, financialCategories } from "../../services/finance";
import FinanceSelect from "./FinanceSelect";
import { dateLabel } from "./financeLabels";
import "./finance.css";
export default function CashFlowPage() {
  const { has } = useAuth();
  const [params] = useSearchParams();
  const [filters,setFilters]=useState(() => params.get("kind") === "ACTUAL" ? {
    kind: "ACTUAL", from: params.get("from") || "", to: params.get("to") || "",
  } : {}); const [page,setPage]=useState(0);
  const state=useProductResource(async (options)=> {
    const [records,summary,balance,banks,categories]=await Promise.all([
      cashFlow.search({...filters,page,size:25},options),cashFlow.summary(filters,options),
      cashFlow.balance({bankAccountId:filters.bankAccountId,through:filters.to},options),
      bankAccounts.options({},options),financialCategories.options({},options)]);
    return {records,summary,balance,banks,categories};
  },[filters,page],has("finance.read"));
  const change=(key,value)=>{setFilters({...filters,[key]:value});setPage(0);};
  const data=state.data;
  if(!has("finance.read")) return <p>Sem permissão para consultar o financeiro.</p>;
  return <div className="finance-page"><PageHeading title="Fluxo de caixa" total={data?.records.totalElements} loading={state.loading} singular="evento" plural="eventos" />
    <section className="panel finance-filters">
      <label>De<input type="date" value={filters.from||""} onChange={e=>change("from",e.target.value)} /></label>
      <label>Até<input type="date" min={filters.from} value={filters.to||""} onChange={e=>change("to",e.target.value)} /></label>
      <FinanceSelect label="Conta" value={filters.bankAccountId||""} values={Object.fromEntries((data?.banks||[]).map(b=>[b.id,b.name]))} onChange={e=>change("bankAccountId",e.target.value)} />
      <FinanceSelect label="Categoria" value={filters.categoryId||""} values={Object.fromEntries((data?.categories||[]).map(c=>[c.id,c.name]))} onChange={e=>change("categoryId",e.target.value)} />
      <FinanceSelect label="Tipo" value={filters.kind||""} values={{ACTUAL:"Realizado",PROJECTED:"Previsto"}} onChange={e=>change("kind",e.target.value)} />
      <FinanceSelect label="Origem" value={filters.direction||""} values={{RECEIVABLE:"Contas a receber",PAYABLE:"Contas a pagar"}} onChange={e=>change("direction",e.target.value)} />
      <button className="secondary" onClick={()=>{setFilters({});setPage(0);}}>Limpar filtros</button>
    </section><ErrorAlert message={state.error} retry={state.reload} />
    {state.loading?<p role="status">Carregando caixa…</p>:data&&<>
      <div className="finance-summary"><p className="panel">Realizado no filtro<br/><strong>{money(data.summary.actual.totalAmount)}</strong></p>
      <p className="panel">Previsto no filtro<br/><strong>{money(data.summary.projected.totalAmount)}</strong></p>
      <p className="panel">Saldo até {dateLabel(data.balance.through)}<br/><strong>{money(data.balance.balance)}</strong><br/><small>Saldo inicial {money(data.balance.initialBalance)} + movimentos {money(data.balance.movements)}. Considera a conta selecionada e todo o histórico até a data, sem os demais filtros.</small></p></div>
      <p className="field-hint">Entradas positivas, saídas negativas. Previsões não possuem conta bancária e ficam fora do filtro por conta.</p>
      <section className="panel table-panel"><div className="table-scroll"><table><thead><tr><th>Data</th><th>Tipo</th><th>Origem</th><th>Conta financeira</th><th>Banco/caixa</th><th>Valor</th></tr></thead>
      <tbody>{data.records.content.map(e=><tr key={e.id}><td>{dateLabel(e.occurredOn)}</td><td>{e.kind==="ACTUAL"?e.reversalId?"Estorno":"Realizado":"Previsto"}</td><td>{e.direction==="RECEIVABLE"?"Receber":"Pagar"}</td><td>#{e.titleId}</td><td>{e.bankAccountId?`#${e.bankAccountId}`:"A definir"}</td><td>{money(e.amount)}</td></tr>)}</tbody></table></div>{!data.records.content.length&&<p>Nenhum movimento encontrado.</p>}</section>
      <Pagination page={page} totalPages={data.records.totalPages} change={setPage}/>
    </>}
  </div>;
}
