import { money } from "../Products/productFormData";
import { dateLabel } from "./financeLabels";
export default function BoletoDemo({ title }) {
  const reference = `JAMSHUB-DEMO-${title.id}`;
  const download = () => {
    // Only server IDs, dates and formatted amounts enter the static document; no arbitrary HTML.
    const html = `<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>Boleto demonstrativo — não processável</title>
      <style>body{font:18px sans-serif;max-width:720px;margin:50px auto;padding:24px;border:3px solid #444}strong{display:block;margin:25px 0}</style>
      <h1>Boleto demonstrativo</h1><strong>NÃO PROCESSÁVEL — SEM VALIDADE BANCÁRIA</strong>
      <p>Referência interna: ${reference}</p><p>Valor: ${money(title.amount)}</p><p>Vencimento: ${dateLabel(title.dueDate)}</p>
      <p>Este documento não pode ser pago em bancos. Não contém linha digitável, código de barras ou confirmação de pagamento.</p>
      <footer>Boleto demonstrativo — não processável. Registro interno JamsHub.</footer></html>`;
    const url = URL.createObjectURL(new Blob([html], { type: "text/html;charset=utf-8" }));
    const link = document.createElement("a"); link.href = url; link.download = `boleto-demonstrativo-nao-processavel-${title.id}.html`;
    link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return <section className="panel"><h3>Boleto demonstrativo — não processável</h3>
    <p>Sem validade bancária. Referência interna: {reference}</p>
    <p>{money(title.amount)} · Vencimento: {dateLabel(title.dueDate)}</p>
    <button className="secondary" onClick={download}>Baixar documento para impressão</button>
  </section>;
}
