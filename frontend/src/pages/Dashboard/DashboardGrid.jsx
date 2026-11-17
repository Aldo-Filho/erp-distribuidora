import { useCallback, useEffect, useRef, useState } from "react";
import GridLayout from "react-grid-layout/legacy";
import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";
import DashboardWidget from "./DashboardWidget";
import { pack } from "./dashboardCatalog";
export default function DashboardGrid({widgets,layout,catalog,editing,onLayout,onDetails}) {
  const container=useRef(null),[width,setWidth]=useState(0);
  useEffect(()=>{const observer=new ResizeObserver(entries=>setWidth(Math.floor(entries[0].contentRect.width)));observer.observe(container.current);return ()=>observer.disconnect();},[]);
  const [contentHeights,setContentHeights]=useState({});
  const measure=useCallback((id,height)=>{
    setContentHeights(current=>current[id]===height?current:{...current,[id]:height});
  },[]);
  const visible=layout.filter(w=>w.visible&&widgets.some(d=>d.id===w.id)).sort((a,b)=>a.y-b.y||a.x-b.x);
  function change(id,action) {
    const ordered=[...layout].sort((a,b)=>a.y-b.y||a.x-b.x);
    const at=ordered.findIndex(w=>w.id===id);
    if(action==="hide") ordered[at]={...ordered[at],visible:false};
    else if(action==="width") ordered[at]={...ordered[at],w:ordered[at].w===12?Math.max(6,catalog.find(d=>d.id===id).minW):12};
    else {
      const target=visible[visible.findIndex(w=>w.id===id)+action];
      if(!target)return;
      const to=ordered.findIndex(w=>w.id===target.id);[ordered[at],ordered[to]]=[ordered[to],ordered[at]];
    }
    onLayout(pack(ordered));
  }
  const content=(w,i)=><DashboardWidget widget={widgets.find(d=>d.id===w.id)} editing={editing} onDetails={onDetails} onMove={step=>change(w.id,step)} onWidth={()=>change(w.id,"width")} onHide={()=>change(w.id,"hide")} first={i===0} last={i===visible.length-1} onContentHeight={measure}/>;
  const update=items=>{
    if(!editing)return;
    const next=layout.map(w=>{const item=items.find(i=>i.i===w.id);return item?{...w,x:item.x,y:item.y,w:item.w,h:Math.min(24,item.h)}:w;}).sort((a,b)=>a.y-b.y||a.x-b.x);
    if(JSON.stringify(next)!==JSON.stringify(layout))onLayout(next);
  };
  return <div ref={container} className="dashboard-grid-container">
    {!visible.length?<section className="panel">Nenhum bloco selecionado. Personalize o painel para exibir indicadores.</section>:width<700?<div className={width>=340?"dashboard-mobile-grid dashboard-mobile-grid--two-columns":"dashboard-mobile-grid"}>
      {visible.map((w,i)=>{
        const compact = w.w <= 6 && catalog.find(d => d.id === w.id)?.kind === "STAT";
        return <div className={compact ? "dashboard-mobile-stat" : undefined} key={w.id}>{content(w,i)}</div>;
      })}
    </div>:<GridLayout width={width} cols={12} rowHeight={40} margin={[16,16]} containerPadding={[0,0]} compactType="vertical" isDraggable={editing} isResizable={editing} draggableHandle=".dashboard-drag" onDragStop={update} onResizeStop={update}
      layout={visible.map(w=>{
        const d=catalog.find(x=>x.id===w.id);
        // A row is 40px plus 16px between rows. Content sets the lower resize bound.
        const minH=Math.max(d.minH,Math.ceil(((contentHeights[w.id]||0)+16)/56));
        return {i:w.id,x:w.x,y:w.y,w:w.w,h:Math.max(w.h,minH),minW:d.minW,minH,maxW:12,maxH:Math.max(24,minH)};
      })}>
      {visible.map((w,i)=><div key={w.id}>{content(w,i)}</div>)}
    </GridLayout>}
  </div>;
}
