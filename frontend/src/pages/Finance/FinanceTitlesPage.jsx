import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import { useProductResource } from "../Products/useProductResource";
import { money } from "../Products/productFormData";
import { receivables, payables, financialCategories } from "../../services/finance";
import FinanceSelect from "./FinanceSelect";
import FinancialTitleDetails from "./FinancialTitleDetails";
import { statusLabels, paymentLabels, dateLabel } from "./financeLabels";
import "./finance.css";

export default function FinanceTitlesPage({ purchase = false }) {
  const { has } = useAuth();
  const [params] = useSearchParams();
  const [filters, setFilters] = useState(() => params.get("status") === "OPEN" ? { status: "OPEN" } : {});
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const service = purchase ? payables : receivables;
  const documentId = params.get("documentId") || "";
  const query = { ...filters, documentId };
  const state = useProductResource(async (options) => {
    const [records, totals, categories] = await Promise.all([
      service.search({ ...query, page, size: 25 }, options), service.summary(query, options),
      financialCategories.options({ type: purchase ? "EXPENSE" : "REVENUE" }, options),
    ]);
    return { records, totals, categories };
  }, [purchase, filters, documentId, page], has("finance.read"));
  const change = (key, value) => { setFilters({ ...filters, [key]: value }); setPage(0); };
  const data = state.data;
  if (!has("finance.read")) return <p>Sem permissão para consultar o financeiro.</p>;
  return <div className="finance-page">
    <PageHeading title={purchase ? "Contas a pagar" : "Contas a receber"} total={data?.records.totalElements} loading={state.loading} singular="conta" plural="contas" />
    {documentId && <p>Contas da {purchase ? "compra" : "venda"} #{documentId}</p>}
    <section className="panel finance-filters" aria-label="Filtros financeiros">
      <FinanceSelect label="Situação" value={filters.status || ""} values={statusLabels} onChange={(e) => change("status", e.target.value)} />
      <label>Vencimento de<input type="date" value={filters.dueFrom || ""} onChange={(e) => change("dueFrom", e.target.value)} /></label>
      <label>Vencimento até<input type="date" min={filters.dueFrom} value={filters.dueTo || ""} onChange={(e) => change("dueTo", e.target.value)} /></label>
      <label>{purchase ? "Código do fornecedor" : "Código do cliente"}<input type="number" min="1" step="1" value={filters.partyId || ""} onChange={(e) => change("partyId", e.target.value)} /></label>
      <FinanceSelect label="Pagamento" value={filters.paymentMethod || ""} values={paymentLabels} onChange={(e) => change("paymentMethod", e.target.value)} />
      <FinanceSelect label="Categoria" value={filters.categoryId || ""} values={Object.fromEntries((data?.categories || []).map((c) => [c.id, c.name]))} onChange={(e) => change("categoryId", e.target.value)} />
      <button className="secondary" onClick={() => { setFilters({}); setPage(0); }}>Limpar filtros</button>
    </section>
    <ErrorAlert message={state.error} retry={state.reload} />
    {state.loading ? <p role="status">Carregando contas…</p> : data && <>
      <p className="panel">Valor nominal das {data.totals.count} contas filtradas: <strong>{money(data.totals.totalAmount)}</strong><br /><small>Inclui os estados selecionados; não representa saldo pendente.</small></p>
      <section className="panel table-panel"><div className="table-scroll"><table>
        <thead><tr><th>Conta / documento</th><th>{purchase ? "Fornecedor" : "Cliente"}</th><th>Vencimento</th><th>Situação</th><th>Valor</th><th>Ações</th></tr></thead>
        <tbody>{data.records.content.map((title) => <tr key={title.id}>
          <td>#{title.id}<small>{title.description}</small></td><td>#{purchase ? title.supplierId : title.customerId}</td>
          <td>{dateLabel(title.dueDate)}</td><td>{statusLabels[title.status]}</td><td>{money(title.amount)}</td>
          <td><button className="secondary" onClick={() => setSelected(title.id)}>Detalhes e histórico</button></td>
        </tr>)}</tbody>
      </table></div>{!data.records.content.length && <p>Nenhuma conta encontrada.</p>}</section>
      <Pagination page={page} totalPages={data.records.totalPages} change={setPage} />
      {page > 0 && !data.records.content.length && <button onClick={() => setPage(0)}>Voltar à primeira página</button>}
    </>}
    {selected && <FinancialTitleDetails key={`${purchase}:${selected}`} id={selected} purchase={purchase} onClose={() => setSelected(null)} onChanged={state.reload} />}
  </div>;
}
