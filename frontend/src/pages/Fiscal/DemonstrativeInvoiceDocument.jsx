import { notice, money, quantity, date, issued, payments, addressLine, addressType, attributesLine } from "./invoicePresentation";
function Party({title,party}) {
  return <section className="demo-invoice-party"><h3>{title}</h3><strong>{party.legalName||"Não informado na emissão"}</strong>
    {party.tradeName&&<p>{party.tradeName}</p>}<p>Documento: {party.taxId||"Não informado"}</p><p>Inscrição estadual: {party.stateRegistration||"Não informada"}</p>
    {party.addresses.length?party.addresses.map((a,index)=><p key={index}><span className="field-hint">{addressType[a.type]||a.type}: </span>{addressLine(a)}</p>):<p className="field-hint">Endereço não informado na emissão.</p>}
  </section>;
}
export default function DemonstrativeInvoiceDocument({document}) {
  const s=document.snapshot;
  return <article className="demo-invoice-document">
    <p className="demo-invoice-banner" role="note">{notice}</p>
    <header><h2>NF-e demonstrativa {document.number}</h2><p>Venda #{document.orderId} · {issued(document.issuedAt)} · São Paulo</p></header>
    <p className="field-hint">Documento de demonstração. Sem chave de acesso, protocolo, QR code ou autorização SEFAZ. Não apura tributos.</p>
    {s.warnings.length>0&&<div className="demo-invoice-warnings" role="note">{s.warnings.map((warning,index)=><p key={index}>{warning}</p>)}</div>}
    <div className="demo-invoice-parties"><Party title="Emissor — dados na emissão" party={s.issuer}/><Party title="Destinatário — dados na emissão" party={s.customer}/></div>
    <div className="demo-invoice-items" tabIndex={0} aria-label="Itens preservados da venda"><table><caption>Produtos no despacho · sem validade fiscal</caption>
      <thead><tr><th scope="col">Produto / apresentação</th><th scope="col">Quantidade</th><th scope="col">Preço unitário</th><th scope="col">Bruto</th><th scope="col">Desconto</th><th scope="col">Líquido</th></tr></thead>
      <tbody>{s.items.map(i=><tr key={i.orderItemId}><td><strong>{i.sku}</strong><small>{i.name}</small>{Object.keys(i.attributes||{}).length>0&&<small>{attributesLine(i.attributes)}</small>}<small>{i.presentation||"BASE"}</small></td><td>{quantity(i.quantity)}<small>BASE: {quantity(i.baseQuantity)} {i.baseUnit||"Não informada"}</small><small>Fator: {quantity(i.baseFactor)}</small></td><td>{money(i.unitPrice)}</td><td>{money(i.grossAmount)}</td><td>{money(i.discountAmount)}<small>{i.discountType==="PERCENTAGE"?`${quantity(i.discount)}%`:"Fixo"}</small></td><td>{money(i.totalAmount)}</td></tr>)}</tbody>
    </table></div>
    <dl className="demo-invoice-totals"><div><dt>Produtos</dt><dd>{money(s.productsAmount)}</dd></div><div><dt>Descontos</dt><dd>{money(s.discountAmount)}</dd></div><div><dt>Total</dt><dd>{money(s.totalAmount)}</dd></div></dl>
    <p>Pagamento: {payments[s.paymentMethod]||"Não informado"} · Vencimento: {date(s.paymentDueDate)}</p>
    <p className="field-hint">Valores capturados no despacho; este documento não comprova pagamento. Os dados permanecem preservados após mudanças nos cadastros.</p>
    <footer className="demo-invoice-banner">{notice}</footer>
  </article>;
}
