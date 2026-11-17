import { useState } from "react";
import useAuth from "../../auth/useAuth";
import { dashboard } from "../../services/dashboard";
import useDashboard from "./useDashboard";
import DashboardFilters from "./DashboardFilters";
import DashboardGrid from "./DashboardGrid";
import DashboardCustomization from "./DashboardCustomization";
import DashboardDetailsDialog from "./DashboardDetailsDialog";
import { defaults, today, knownIds } from "./dashboardCatalog";
import "./dashboard.css";
export default function DashboardPage() {
  const {session,has}=useAuth();
  const identity=JSON.stringify([session?.userId,[...(session?.permissionKeys||[])].sort()]);
  return session?<Dashboard key={identity} has={has}/>:null;
}
function Dashboard({has}) {
  const state=useDashboard();
  const [draft,setDraft]=useState(null),[snapshot,setSnapshot]=useState(null),[busy,setBusy]=useState(false),[notice,setNotice]=useState(""),[conflict,setConflict]=useState(false),[detail,setDetail]=useState(null);
  const editing=draft!==null;
  const widgets=(state.data?.widgets||[]).filter(w=>knownIds.has(w.id)&&w.permissions.every(has));
  const catalog=(state.data?.catalog||[]).filter(d=>knownIds.has(d.id)&&d.permissions.every(has));
  const allowed=new Set(catalog.map(d=>d.id));
  const layout=(draft||state.preference?.widgets||[]).filter(w=>allowed.has(w.id));
  const enter=()=>{setSnapshot(structuredClone(state.filter));setDraft(structuredClone(layout));setNotice("");};
  const cancel=()=>{state.setFilter(snapshot);setDraft(null);setNotice("Edição cancelada.");};
  async function save() {
    if(state.loading||state.error||!state.data||!state.preference||conflict)return;
    setBusy(true);setNotice("");
    try {
      const filter={...state.filter};if(filter.preset!=="CUSTOM"){filter.from=null;filter.to=null;}
      const saved=await dashboard.save({schemaVersion:1,revision:state.preference.revision,widgets:layout,filter});
      state.setPreference(saved);setDraft(null);setNotice("Painel salvo para seu usuário.");setConflict(false);
    }catch(e){setNotice(e.status===409?"Outra aba alterou este painel. Recarregue a configuração antes de salvar.":e.message);setConflict(e.status===409);}
    finally{setBusy(false);}
  }
  async function reloadPreference() {
    setBusy(true);
    try{const p=await dashboard.preference();state.setPreference(p);state.setFilter(p.filter);setDraft(null);setConflict(false);setNotice("Configuração recarregada.");}catch(e){setNotice(e.message);}finally{setBusy(false);}
  }
  async function reset() {
    if(editing){setDraft(defaults(catalog));state.setFilter({preset:"MONTH",referenceDate:today(),from:null,to:null,compare:false});return;}
    setBusy(true);
    try{const p=await dashboard.reset(state.preference.revision);state.setPreference(p);state.setFilter(p.filter);setNotice("Padrão restaurado no servidor.");setConflict(false);}catch(e){setNotice(e.status===409?"Outra aba alterou este painel. Recarregue a configuração antes de restaurar.":e.message);setConflict(e.status===409);}finally{setBusy(false);}
  }
  return <div className="dashboard-page" aria-busy={busy}>
    <header className="page-title"><div><span className="dashboard-eyebrow">Seu espaço de trabalho</span><h1>Visão geral</h1><p className="field-hint">Indicadores conforme seus acessos{state.data?.roleName?` · ${state.data.roleName}`:""}</p></div>
      <div className="dashboard-actions"><button className="secondary" disabled={busy||state.loading} onClick={state.reload}>Atualizar</button>
      {state.preference&&<><button disabled={busy||state.loading||!!state.error||conflict} onClick={editing?save:enter}>{busy?"Salvando…":editing?"Salvar painel":"Personalizar"}</button>
      {editing?<button className="secondary" disabled={busy} onClick={cancel}>Cancelar</button>:<button className="secondary" disabled={busy||state.loading||!!state.error||conflict} onClick={save}>Salvar filtros</button>}
      <button className="secondary" disabled={busy||state.loading} onClick={reset}>Restaurar padrão{editing?" (rascunho)":" salvo"}</button></>}
      </div>
    </header>
    {notice&&<div className="panel" role="status">{notice}{conflict&&<button disabled={busy} onClick={reloadPreference}>Recarregar configuração</button>}</div>}
    {state.filter&&<DashboardFilters filter={state.filter} onChange={state.setFilter} selection={state.data?.selection} options={state.options} setOptions={state.setOptions} has={has} disabled={busy}/>}
    {state.error&&<div className="panel" role="alert">{state.error}<button onClick={state.reload}>Tentar novamente</button></div>}
    {!!state.data?.coverage.undatedEligibleSales&&<p className="dashboard-warning" role="status">Cobertura incompleta: {state.data.coverage.undatedEligibleSales} vendas confirmadas/despachadas sem data de confirmação estão fora das métricas por período.</p>}
    {!!state.data?.coverage.incompleteProductItems&&<p className="dashboard-warning">{state.data.coverage.incompleteProductItems} itens com conversão ou unidade incompleta estão fora do ranking por quantidade e preço médio. O ranking por faturamento mantém esses valores com a unidade disponível ou a indicação de dados incompletos.</p>}
    {state.data?.coverage.unavailable&&<p className="dashboard-warning">Não foi possível consultar a cobertura histórica.</p>}
    {editing&&catalog.length>0&&<DashboardCustomization catalog={catalog} layout={layout} onChange={setDraft}/>}
    {state.loading?<div className="panel dashboard-loading" role="status">Carregando seus indicadores…</div>:!state.error&&state.data&&<>
      {!widgets.length?<section className="panel"><h2>Nenhum indicador disponível</h2><p>Os blocos dependem das permissões de consulta do seu cargo.</p></section>:<DashboardGrid widgets={widgets} layout={layout} catalog={catalog} editing={editing&&!busy} onLayout={setDraft} onDetails={(widget,productId)=>setDetail({widget,productId})}/>}
      <p className="field-hint">Consulta em {new Date(state.data.generatedAt).toLocaleString("pt-BR")}. Faturamento por confirmação; recebimentos e pagamentos por movimento. Movimento líquido não representa saldo bancário. Estoque e vencimentos representam a situação atual.</p>
    </>}
    {detail&&<DashboardDetailsDialog key={`${detail.widget.id}-${detail.productId||""}`} {...detail} filter={state.request} onClose={()=>setDetail(null)}/>}
  </div>;
}
