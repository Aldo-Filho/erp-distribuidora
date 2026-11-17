import { useLayoutEffect, useRef } from "react";
import DashboardCharts from "./DashboardCharts";
import DashboardProductRanking from "./DashboardProductRanking";
import { bases, displayDate, format, displayLabel } from "./dashboardCatalog";
export default function DashboardWidget({widget,onDetails,editing,onMove,onWidth,onHide,first,last,onContentHeight}) {
  const {comparison}=widget;
  const content=useRef(null);
  useLayoutEffect(()=>{
    const element=content.current;
    const measure=()=>{
      const style=getComputedStyle(element.parentElement);
      const inset=[style.paddingTop,style.paddingBottom,style.borderTopWidth,style.borderBottomWidth]
        .reduce((total,value)=>total+(parseFloat(value)||0),0);
      onContentHeight(widget.id,Math.ceil(element.getBoundingClientRect().height+inset));
    };
    const observer=new ResizeObserver(measure);
    observer.observe(element);
    measure();
    return ()=>observer.disconnect();
  },[widget.id,onContentHeight]);
  return <article className="panel dashboard-widget">
    <div className="dashboard-widget-content" ref={content}>
    {editing&&<div className="dashboard-widget-tools"><span className="dashboard-drag" tabIndex={-1}>⠿ Arrastar</span><button disabled={first} onClick={()=>onMove(-1)} aria-label={`Mover ${widget.title} para antes`}>←</button><button disabled={last} onClick={()=>onMove(1)} aria-label={`Mover ${widget.title} para depois`}>→</button><button onClick={onWidth}>Largura</button><button onClick={onHide}>Ocultar</button></div>}
    <header><span className="dashboard-eyebrow">{bases[widget.dateBasis]}</span><h2>{widget.title}</h2></header>
    <p className="dashboard-period">{displayDate(widget.period.from)} a {displayDate(widget.period.to)}</p>
    {widget.state==="UNAVAILABLE"?<p role="status">Indicador indisponível. Atualize para tentar novamente.</p>:<>
      {widget.metric!=null&&<p className="dashboard-value">{format(widget.metric,widget.unit)}</p>}
      {comparison&&<p className="dashboard-comparison">{comparison.changePercent==null?"Sem base de comparação":`${format(comparison.changePercent)}% vs. anterior`} <span>· {format(comparison.previousValue,widget.unit)}</span></p>}
      {widget.state==="EMPTY"?<p className="field-hint">Sem registros ou valor no recorte.</p>:widget.kind==="RANKING"?<DashboardProductRanking widget={widget} onDetails={productId=>onDetails(widget,productId)}/>:widget.kind==="AGENDA"?<div className="dashboard-agenda">{widget.rows.map(row=><div key={row.id}><span>{displayLabel(row.label)}<small>{displayDate(row.day)}</small></span><strong>{format(row.value,row.unit)}</strong></div>)}</div>:widget.kind!=="STAT"&&<DashboardCharts widget={widget}/>}
      {widget.rows.length>0&&!["STAT","LINE"].includes(widget.kind)&&<p className="field-hint">{widget.kind==="RANKING"?"Top 10":widget.id==="stock.composition"||widget.kind==="AGENDA"?"Prévia de até 8 registros":"Distribuição de até 50 grupos"}. Consulte a tabela para todos os registros.</p>}
      {widget.drillDown&&<button className="dashboard-origin" onClick={()=>onDetails(widget)}>Consultar origem e dados em tabela →</button>}
    </>}
    </div>
  </article>;
}
