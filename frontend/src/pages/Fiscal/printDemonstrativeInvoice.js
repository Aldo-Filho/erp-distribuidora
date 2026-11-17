import { notice, money, quantity, issued, date, payments, addressLine, addressType, attributesLine } from "./invoicePresentation";
const escape=value=>String(value??"Não informado").replace(/[&<>"']/g,char=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"})[char]);
function party(title,p) {
  return `<section class="party"><h2>${escape(title)}</h2><strong>${escape(p.legalName)}</strong>
    ${p.tradeName?`<p>${escape(p.tradeName)}</p>`:""}<p>Documento: ${escape(p.taxId)} · IE: ${escape(p.stateRegistration)}</p>
    ${p.addresses.length?p.addresses.map(a=>`<p>${escape(addressType[a.type]||a.type)}: ${escape(addressLine(a))}</p>`).join(""):"<p>Endereço não informado na emissão.</p>"}</section>`;
}
export function printDemonstrativeInvoice(document) {
  const popup=window.open("","_blank","width=1050,height=800");
  if(!popup) throw new Error("Permita abrir a janela de impressão para este sistema e tente novamente.");
  popup.opener=null;
  const s=document.snapshot;
  // Every snapshot field is escaped; the print window loads no scripts or remote resources.
  const html=`<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>${escape(document.number)} — sem validade fiscal</title>
    <style>
    *{box-sizing:border-box}body{font:12px Arial,sans-serif;color:#172c36;margin:24px}h1{font-size:23px}h2{font-size:14px;margin:8px 0}p{margin:5px 0;overflow-wrap:anywhere}
    .banner{padding:12px;border:2px solid #172c36;font-weight:bold;text-align:center}.parties{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin:18px 0}.party{border:1px solid #bbb;padding:12px;break-inside:avoid}
    table{border-collapse:collapse;width:100%;margin:16px 0;table-layout:fixed}th,td{padding:8px 5px;border-bottom:1px solid #bbb;text-align:left;overflow-wrap:anywhere;vertical-align:top}th:first-child{width:29%}small{display:block;margin-top:4px}thead{display:table-header-group}tr{break-inside:avoid}
    .totals{text-align:right;break-inside:avoid}.warning{border-left:3px solid #555;padding-left:10px}.watermark{display:none}.footer{margin-top:20px;font-weight:bold}
    @page{size:A4;margin:16mm 12mm 22mm}@media print{body{margin:0}.watermark{display:block;position:fixed;top:48%;left:5%;width:90%;text-align:center;transform:rotate(-25deg);font-size:29px;font-weight:bold;opacity:.08;pointer-events:none}.footer{position:fixed;bottom:-14mm;left:0;width:100%;text-align:center;font-size:10px}}
    </style></head><body>
    <div class="watermark">${escape(notice)}</div><div class="banner">${escape(notice)}</div>
    <h1>NF-e demonstrativa ${escape(document.number)}</h1><p>Venda #${escape(document.orderId)} · Emitido em ${escape(issued(document.issuedAt))} · America/Sao_Paulo</p>
    <p>Este documento de demonstração não é uma NF-e autorizada. Não possui chave de acesso, protocolo, QR code ou autorização SEFAZ. Não apura tributos.</p>
    ${s.warnings.map(w=>`<p class="warning">${escape(w)}</p>`).join("")}
    <div class="parties">${party("Emissor — dados preservados",s.issuer)}${party("Destinatário — dados preservados",s.customer)}</div>
    <table><thead><tr><th>Produto / apresentação</th><th>Quantidade</th><th>Preço unitário</th><th>Bruto</th><th>Desconto</th><th>Líquido</th></tr></thead><tbody>
    ${s.items.map(i=>`<tr><td><strong>${escape(i.sku)}</strong><small>${escape(i.name)}</small>${Object.keys(i.attributes||{}).length?`<small>${escape(attributesLine(i.attributes))}</small>`:""}<small>${escape(i.presentation||"BASE")}</small></td><td>${escape(quantity(i.quantity))}<small>BASE: ${escape(quantity(i.baseQuantity))} ${escape(i.baseUnit)}</small><small>Fator: ${escape(quantity(i.baseFactor))}</small></td><td>${escape(money(i.unitPrice))}</td><td>${escape(money(i.grossAmount))}</td><td>${escape(money(i.discountAmount))}<small>${i.discountType==="PERCENTAGE"?escape(quantity(i.discount))+"%":"Fixo"}</small></td><td>${escape(money(i.totalAmount))}</td></tr>`).join("")}
    </tbody></table>
    <section class="totals"><p>Produtos: ${escape(money(s.productsAmount))}</p><p>Descontos: ${escape(money(s.discountAmount))}</p><h2>Total: ${escape(money(s.totalAmount))}</h2></section>
    <p>Pagamento: ${escape(payments[s.paymentMethod]||s.paymentMethod)} · Vencimento: ${escape(date(s.paymentDueDate))}</p>
    <p>Os valores descrevem a venda no despacho; este documento não comprova pagamento. Alterações posteriores nos cadastros não modificam estes dados.</p>
    <div class="footer">${escape(notice)} · ${escape(document.number)} · Documento de demonstração</div>
    </body></html>`;
  popup.document.write(html);
  popup.document.close();
  popup.addEventListener("afterprint",()=>popup.close(),{once:true});
  popup.focus();
  popup.requestAnimationFrame(()=>popup.requestAnimationFrame(()=>popup.print()));
}
