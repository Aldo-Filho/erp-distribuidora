import { useEffect, useState } from "react";
import Modal from "../../components/feedback/Modal";
import { dashboard } from "../../services/dashboard";
import { bases, displayDate, format, displayLabel } from "./dashboardCatalog";
const labels={id:"Identificação",label:"Origem",day:"Data",value:"Valor / quantidade",unit:"Unidade",quantity:"Quantidade base",revenue:"Faturamento",sku:"SKU",base_unit:"Unidade base",physical:"Físico",reserved:"Reservado",available:"Disponível",minimum:"Mínimo",order_id:"Venda",product_id:"Produto"};
export default function DashboardDetailsDialog({widget,filter,productId,onClose}) {
  const [page,setPage]=useState(0),[state,setState]=useState({loading:true}),[tick,setTick]=useState(0);
  useEffect(()=>{
    const controller=new AbortController();let active=true;
    setState({loading:true});
    dashboard.details(widget.id,{...filter,productId},page,{signal:controller.signal}).then(data=>{if(active)setState({data});})
      .catch(e=>{if(active&&e.name!=="AbortError")setState({error:e.message});});
    return ()=>{active=false;controller.abort();};
  },[widget.id,filter,productId,page,tick]);
  const data=state.data;
  const columns=data?.content.length?Object.keys(data.content[0]):[];
  return <Modal title={widget.title} onClose={onClose} wide>
    <p className="field-hint">{bases[widget.dateBasis]} · {data?`${displayDate(data.period.from)} a ${displayDate(data.period.to)}`:""} · Mesmo recorte do indicador. Dados consultados novamente ao abrir.</p>
    {widget.kind==="RANKING"&&<p className="field-hint">Itens das vendas de origem. Preço médio ponderado = soma do faturamento líquido dos itens ÷ soma das quantidades base. Cada linha mantém a unidade histórica.</p>}
    {state.loading&&<p role="status">Carregando origem…</p>}
    {state.error&&<div role="alert"><p>{state.error}</p><button onClick={()=>setTick(t=>t+1)}>Tentar novamente</button></div>}
    {data&&<>{!data.content.length?<p>Nenhum registro neste recorte.</p>:<div className="dashboard-table-scroll"><table><caption>Origem do indicador · página {page+1} · {data.totalElements} registros</caption><thead><tr>{columns.map(key=><th key={key} scope="col">{labels[key]||key}</th>)}</tr></thead><tbody>{data.content.map((row,index)=><tr key={`${row.id}-${index}`}>{columns.map(key=><td key={key}>{key==="day"?displayDate(row[key]):typeof row[key]==="number"?format(row[key],["value","revenue"].includes(key)?(key==="revenue"?"BRL":row.unit):"COUNT"):displayLabel(row[key])}</td>)}</tr>)}</tbody></table></div>}
      <div className="dashboard-actions"><button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Anterior</button><span>Página {page+1}</span><button disabled={(page+1)*data.size>=data.totalElements} onClick={()=>setPage(p=>p+1)}>Próxima</button></div></>}
  </Modal>;
}
