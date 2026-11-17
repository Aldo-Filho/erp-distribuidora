import { useState } from "react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import Modal from "../../components/feedback/Modal";
import { useProductResource } from "../Products/useProductResource";
import useProductAction from "../Products/useProductAction";
import { money } from "../Products/productFormData";
import { bankAccounts, financialCategories } from "../../services/finance";
import { categoryTypes, bankTypes } from "./financeLabels";
import FinanceSelect from "./FinanceSelect";
import "./finance.css";
export default function FinancialConfigurationPage({ bank = false }) {
  const { has } = useAuth();
  const [page, setPage] = useState(0);
  const [active, setActive] = useState("");
  const [flow, setFlow] = useState(null);
  const service = bank ? bankAccounts : financialCategories;
  const state = useProductResource((options) => service.search({ page, size: 25, active }, options), [bank, page, active], has("finance.configure"));
  if (!has("finance.configure")) return <p>Sem permissão de configuração financeira.</p>;
  return <div className="finance-page"><header className="page-title"><PageHeading title={bank ? "Contas bancárias e caixa" : "Categorias financeiras"} total={state.data?.totalElements} loading={state.loading} singular="registro" plural="registros" />
    <button onClick={() => setFlow({})}>Cadastrar {bank ? "conta" : "categoria"}</button></header>
    <FinanceSelect label="Situação" value={active} values={{ true: "Ativos", false: "Inativos" }} onChange={(e) => { setActive(e.target.value); setPage(0); }} />
    <ErrorAlert message={state.error} retry={state.reload} />
    {state.loading ? <p role="status">Carregando…</p> : state.data && <><section className="panel table-panel"><div className="table-scroll"><table>
      <thead><tr><th>Nome</th><th>Tipo</th><th>{bank ? "Saldo inicial" : "Padrão para documentos"}</th><th>Situação</th><th>Ações</th></tr></thead>
      <tbody>{state.data.content.map((item) => <tr key={item.id}><td>{item.name}</td><td>{(bank ? bankTypes : categoryTypes)[item.type]}</td>
        <td>{bank ? money(item.initialBalance) : item.defaultForDocuments ? "Sim" : "Não"}</td><td>{item.active ? "Ativo" : "Inativo"}</td>
        <td><button className="secondary" onClick={() => setFlow(item)}>Editar</button></td></tr>)}</tbody>
    </table></div>{!state.data.content.length && <p>Nenhum registro encontrado.</p>}</section>
      <Pagination page={page} totalPages={state.data.totalPages} change={setPage} />
      {page > 0 && !state.data.content.length && <button onClick={() => setPage(0)}>Voltar à primeira página</button>}</>}
    {flow && <ConfigurationForm bank={bank} item={flow} onClose={() => setFlow(null)} onSaved={() => { setFlow(null); state.reload(); }} />}
  </div>;
}
function ConfigurationForm({ bank, item, onClose, onSaved }) {
  const [value, setValue] = useState({ name: item.name || "", type: item.type || (bank ? "CASH" : "REVENUE"), initialBalance: item.initialBalance ?? "0.00", active: item.active ?? true, defaultForDocuments: item.defaultForDocuments ?? false });
  const change = (key, next) => setValue({ ...value, [key]: next });
  const action = useProductAction();
  const service = bank ? bankAccounts : financialCategories;
  const [uncertain, setUncertain] = useState(false);
  return <Modal title={`${item.id ? "Editar" : "Cadastrar"} ${bank ? "conta" : "categoria"}`} onClose={onClose} busy={action.pending}>
    <form className="finance-form" onSubmit={(e) => { e.preventDefault(); action.run(async () => {
      const body = bank ? { name: value.name.trim(), type: value.type, initialBalance: value.initialBalance } : { name: value.name.trim(), defaultForDocuments: value.defaultForDocuments };
      if (!bank && !item.id) body.type = value.type;
      if (item.id) body.active = value.active;
      try { if (item.id) await service.update(item.id, body); else await service.register(body); }
      catch (issue) { if (!item.id && (!issue.status || issue.status >= 500)) setUncertain(true); throw issue; }
      onSaved();
    }); }}>
      <ErrorAlert message={action.error} />
      {uncertain && <p role="alert">A criação pode ter sido registrada. Feche e recarregue a lista antes de tentar cadastrar novamente.</p>}
      <fieldset disabled={action.pending || uncertain}>
        <label>Nome<input required maxLength={100} value={value.name} onChange={(e) => change("name", e.target.value)} /></label>
        <FinanceSelect required empty={null} label="Tipo" value={value.type} values={bank ? bankTypes : categoryTypes} disabled={!bank && Boolean(item.id)} onChange={(e) => change("type", e.target.value)} />
        {bank ? <label>Saldo inicial<input required type="number" step="0.01" min="-9999999999.99" max="9999999999.99" value={value.initialBalance} onChange={(e) => change("initialBalance", e.target.value)} /></label> :
          <label><input type="checkbox" checked={value.defaultForDocuments} onChange={(e) => change("defaultForDocuments", e.target.checked)} />Usar como padrão para novos documentos</label>}
        {item.id && <label><input type="checkbox" checked={value.active} onChange={(e) => change("active", e.target.checked)} />Ativo</label>}
      </fieldset>
      <p className="field-hint">{bank ? "Saldo inicial e tipo não podem ser alterados após movimentações." : "A troca da categoria padrão afeta apenas novos documentos. Remova o padrão antes de inativar."}</p>
      <div className="actions"><button type="button" className="secondary" disabled={action.pending} onClick={onClose}>Fechar</button><button disabled={action.pending || uncertain}>{action.pending ? "Salvando…" : "Salvar"}</button></div>
    </form>
  </Modal>;
}
