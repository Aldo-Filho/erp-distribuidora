export const profiles = {FINANCE:"Financeiro",SALES:"Vendas",PURCHASING:"Compras",STOCK:"Estoque"};
export const bases = {CONFIRMED_AT:"Data de confirmação",OCCURRED_ON:"Data do movimento",CREATED_AT:"Data de criação",SNAPSHOT:"Situação atual"};
export const today = () => new Intl.DateTimeFormat("sv-SE",{timeZone:"America/Sao_Paulo",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());
const date = value => new Date(`${value}T12:00:00Z`);
const iso = value => value.toISOString().slice(0,10);
export function resolveFilter(filter) {
  const now=today();
  let from,to;
  const anchor=date(filter.referenceDate);
  if (!Number.isFinite(+anchor)||filter.referenceDate>now) throw new Error("Escolha uma referência até hoje.");
  const year=anchor.getUTCFullYear(),month=anchor.getUTCMonth(),day=anchor.getUTCDate();
  switch(filter.preset) {
    case "DAY": from=to=iso(anchor); break;
    case "WEEK": { const start=new Date(anchor); start.setUTCDate(day-((anchor.getUTCDay()+6)%7)); from=iso(start); start.setUTCDate(start.getUTCDate()+6);to=iso(start);break; }
    case "FORTNIGHT":from=iso(new Date(Date.UTC(year,month,day<=15?1:16,12)));to=iso(new Date(Date.UTC(year,month+(day<=15?0:1),day<=15?15:0,12)));break;
    case "YEAR":from=`${year}-01-01`;to=`${year}-12-31`;break;
    case "CUSTOM":from=filter.from;to=filter.to;break;
    default:from=iso(new Date(Date.UTC(year,month,1,12)));to=iso(new Date(Date.UTC(year,month+1,0,12)));
  }
  if(filter.preset!=="CUSTOM" && to>now) to=now;
  const days=(+date(to)-+date(from))/86400000+1;
  if(!from||!to||!Number.isFinite(days)||days<1||days>366||to>now) throw new Error("Informe um intervalo de até 366 dias, com início até o fim e sem datas futuras.");
  return {from,to,compare:filter.compare};
}
export function format(value,unit="COUNT") {
  if(value==null) return "—";
  return Number(value).toLocaleString("pt-BR",unit.startsWith("BRL")?{style:"currency",currency:"BRL",maximumFractionDigits:2}:{maximumFractionDigits:2});
}
export const displayDate = value => value ? value.slice(0,10).split("-").reverse().join("/") : "—";
// Compact deterministic placement is also the keyboard alternative to collision-aware dragging.
export function pack(widgets) {
  let x=0,y=0,height=0;
  return widgets.map(w=> {
    if(!w.visible) return {...w,x:0,y:0};
    if(x+w.w>12) {y+=height;x=0;height=0;}
    const next={...w,x,y};x+=w.w;height=Math.max(height,w.h);return next;
  });
}
export function defaults(catalog) { return pack(catalog.map(d=>({id:d.id,visible:true,x:0,y:0,w:d.kind==="STAT"?3:6,h:d.minH}))); }

export const knownIds=new Set(["sales.revenue","cash.in","cash.out","cash.net","receivables.balance","payables.balance","finance.overdue","finance.agenda","sales.count","sales.status","sales.dispatch","sales.products","sales.prices","purchases.status","purchases.receiving","purchases.created","purchases.suggestions","stock.low","stock.empty","stock.composition","stock.movements"]);
const labels={PENDING:"Pendente",CONFIRMED:"Confirmada",INVOICED:"Despachada",CANCELED:"Cancelada",DRAFT:"Rascunho",SENT:"Enviada",PARTIALLY_RECEIVED:"Recebida parcialmente",RECEIVED:"Recebida",INBOUND:"Entrada",OUTBOUND:"Saída",ADJUSTMENT:"Ajuste",RECEIVABLE:"A receber",PAYABLE:"A pagar",COUNT:"Registros",DADOS_INCOMPLETOS:"Dados incompletos"};
export function displayLabel(value) { const text=String(value??"—");return text.split(" · ").map(p=>labels[p]||p).join(" · "); }
