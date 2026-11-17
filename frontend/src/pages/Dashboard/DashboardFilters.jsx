import FilterSelect from "../../components/feedback/FilterSelect";
import { today, displayDate } from "./dashboardCatalog";
export default function DashboardFilters({filter,onChange,selection,options,setOptions,has,disabled}) {
  const set=(key,value)=>onChange({...filter,[key]:value});
  return <section className="panel dashboard-filters" aria-label="Filtros do painel">
    <fieldset disabled={disabled} className="dashboard-filter-fields">
      <label>Período<FilterSelect disabled={disabled} aria-label="Período" value={filter.preset} onChange={e=>onChange({...filter,preset:e.target.value,from:null,to:null})}>
        {[["DAY","Dia"],["WEEK","Semana"],["FORTNIGHT","Quinzena"],["MONTH","Mês"],["YEAR","Ano"],["CUSTOM","Personalizado"]].map(([v,l])=><option key={v} value={v}>{l}</option>)}
      </FilterSelect></label>
      {filter.preset!=="CUSTOM"?<label>Referência<input type="date" max={today()} value={filter.referenceDate} onChange={e=>set("referenceDate",e.target.value)}/></label>:<>
        <label>De<input type="date" max={today()} value={filter.from||""} onChange={e=>set("from",e.target.value)}/></label>
        <label>Até<input type="date" max={today()} value={filter.to||""} onChange={e=>set("to",e.target.value)}/></label></>}
      <label className="dashboard-check"><input type="checkbox" checked={filter.compare} onChange={e=>set("compare",e.target.checked)}/>Comparar período anterior</label>
      {(has("orders.read")||has("stock.read"))&&<label>Unidade dos blocos compatíveis<FilterSelect disabled={disabled} aria-label="Unidade dos blocos compatíveis" value={options.unit} onChange={e=>setOptions({...options,unit:e.target.value})}><option value="">Todas no estoque · UN no ranking</option>{["UN","KG","G","L","ML","M"].map(u=><option key={u} value={u}>{u}</option>)}</FilterSelect></label>}
      {has("stock.read")&&<label>Depósito · estoque e sugestões<input type="number" min="1" placeholder="Todos" value={options.warehouseId||""} onChange={e=>setOptions({...options,warehouseId:e.target.value})}/></label>}
      {has("orders.read")&&<label>Ranking<FilterSelect disabled={disabled} aria-label="Ordenar ranking por" value={options.rankingMetric} onChange={e=>setOptions({...options,rankingMetric:e.target.value})}><option value="QUANTITY">Quantidade base</option>{has("finance.read")&&<option value="REVENUE">Faturamento</option>}{has("finance.read")&&<option value="AVERAGE_PRICE">Preço médio ponderado</option>}</FilterSelect></label>}
    </fieldset>
    {selection&&<p className="field-hint">{displayDate(selection.from)} a {displayDate(selection.to)} · America/Sao_Paulo{selection.compare&&` · Anterior: ${displayDate(selection.previousFrom)} a ${displayDate(selection.previousTo)}`}. Blocos de situação atual mantêm a data de hoje.</p>}
  </section>;
}
