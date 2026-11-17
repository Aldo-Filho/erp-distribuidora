import { format, displayLabel } from "./dashboardCatalog";
export default function DashboardProductRanking({widget,onDetails}) {
  const max=Math.max(...widget.rows.map(r=>Number(r.value)),1);
  return <ol className="dashboard-ranking">{widget.rows.map(row=><li key={`${row.id}-${row.unit}`}>
    <button className="dashboard-product" onClick={()=>onDetails(row.id)}>
      <span>{row.label}</span><strong>{format(row.value,row.unit)} {row.unit}</strong>
      <span className="dashboard-rank-track" aria-hidden="true"><span style={{width:`${Math.max(0,Number(row.value))/max*100}%`}}/></span>
      {row.quantity!=null&&<small>{format(row.quantity)} {displayLabel(row.base_unit)} base</small>}
    </button>
  </li>)}</ol>;
}
