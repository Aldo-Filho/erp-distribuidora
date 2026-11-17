import { pack, profiles } from "./dashboardCatalog";
export default function DashboardCustomization({catalog,layout,onChange}) {
  return <section className="panel dashboard-customization"><h2>Escolha seus blocos</h2><p className="field-hint">Arraste pela alça ou use os botões para mover e alterar a largura. No celular, a leitura segue a ordem salva. Alterações só são gravadas ao salvar.</p>
    <div className="dashboard-catalog">{catalog.map(d=><label className="dashboard-check" key={d.id}><input type="checkbox" checked={layout.find(w=>w.id===d.id)?.visible||false} onChange={e=>onChange(pack(layout.map(w=>w.id===d.id?{...w,visible:e.target.checked}:w)))}/><span>{d.title}<small>{profiles[d.profile]}</small></span></label>)}</div>
  </section>;
}
