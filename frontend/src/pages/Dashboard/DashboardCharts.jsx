import { ResponsiveContainer, LineChart, Line, BarChart, Bar, CartesianGrid, XAxis, YAxis, Tooltip, Legend } from "recharts";
import { format, displayLabel, displayDate } from "./dashboardCatalog";
export default function DashboardCharts({widget}) {
  const line=widget.kind==="LINE";
  const data=line?widget.series:widget.rows;
  // Stock composition remains per SKU/depot. Other quantities retain their unit in the label.
  const points=data.map(p=>({...p,label:(/^\d{4}-\d{2}-\d{2}$/.test(p.label)?displayDate(p.label):displayLabel(p.label))+(p.unit&&!["BRL","COUNT"].includes(p.unit)?` · ${p.unit}`:"")}));
  const Chart=line?LineChart:BarChart;
  const composition=widget.id==="stock.composition";
  return <div className="dashboard-chart" role="group" aria-label={`Gráfico: ${widget.title}`}>
    <ResponsiveContainer width="100%" height="100%" minHeight={140}>
      <Chart data={points} margin={{top:8,right:12,bottom:6,left:0}} accessibilityLayer>
        <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false}/>
        <XAxis dataKey="label" tick={{fill:"var(--muted)",fontSize:11}} minTickGap={25}/>
        <YAxis width={65} tick={{fill:"var(--muted)",fontSize:11}} tickFormatter={v=>Number(v).toLocaleString("pt-BR",{notation:"compact"})}/>
        <Tooltip formatter={(v,name)=>[format(v,widget.unit),name]} contentStyle={{background:"var(--surface)",border:"1px solid var(--border)",color:"var(--text)"}}/>
        {line?<><Line name="Selecionado" type="monotone" dataKey="value" stroke="var(--accent)" strokeWidth={2.5} dot={false} isAnimationActive={false}/>{widget.comparison&&<Line name="Anterior · mesma duração" dataKey="previous" stroke="var(--muted)" strokeDasharray="5 5" dot={false} isAnimationActive={false}/>}</>:composition?<>
          <Bar dataKey="physical" name="Físico" fill="var(--accent)" isAnimationActive={false}/><Bar dataKey="reserved" name="Reservado" fill="var(--muted)" isAnimationActive={false}/><Bar dataKey="available" name="Disponível" fill="var(--success-fg)" isAnimationActive={false}/>
        </>:<Bar dataKey="value" name={widget.unit} fill="var(--accent)" radius={[4,4,0,0]} isAnimationActive={false}/>}
        <Legend/>
      </Chart>
    </ResponsiveContainer>
  </div>;
}
