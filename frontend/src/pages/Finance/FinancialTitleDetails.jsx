import { useState } from "react";
import { Link } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import { useProductResource } from "../Products/useProductResource";
import useProductAction from "../Products/useProductAction";
import { money } from "../Products/productFormData";
import { receivables, payables, bankAccounts } from "../../services/finance";
import FinanceSelect from "./FinanceSelect";
import BoletoDemo from "./BoletoDemo";
import { localDate, dateLabel, paymentLabels, statusLabels } from "./financeLabels";

export default function FinancialTitleDetails({ id, purchase, onClose, onChanged }) {
  const { has } = useAuth();
  const [page, setPage] = useState(0);
  const [flow, setFlow] = useState(null);
  const service = purchase ? payables : receivables;
  const state = useProductResource(async (options) => {
    const [title, history] = await Promise.all([service.findOne(id, options), service.history(id, page, options)]);
    const events = await Promise.all(history.content.map(async (payment) => ({ ...payment, reversals: (await service.reversals(id, payment.id, options)).content })));
    return { title, history, events };
  }, [id, purchase, page], has("finance.read"));
  const data = state.data;
  const changed = () => { setFlow(null); state.reload(); onChanged(); };
  if (!has("finance.read")) return null;
  return <Modal title={`Conta a ${purchase ? "pagar" : "receber"} #${id}`} onClose={onClose} busy={Boolean(flow)} wide>
    <div className="finance-page"><ErrorAlert message={state.error} retry={state.reload} />
      {state.loading ? <p role="status">Carregando conta…</p> : data && <>
        <p>{data.title.description} · {statusLabels[data.title.status]}</p>
        <p><strong>{money(data.title.amount)}</strong> · Vence em {dateLabel(data.title.dueDate)} · {paymentLabels[data.title.paymentMethod] || "Método definido na baixa"}</p>
        {has(purchase ? "purchases.read" : "orders.read") && (purchase ? data.title.purchaseId : data.title.orderId) &&
          <Link to={`/${purchase ? "purchases" : "orders"}/${purchase ? data.title.purchaseId : data.title.orderId}`}>Abrir documento de origem</Link>}
        {!purchase && data.title.paymentMethod === "BOLETO" && <BoletoDemo title={data.title} />}
        {has("finance.settle") && ["OPEN", "OVERDUE"].includes(data.title.status) && data.history.totalElements === 0 &&
          <button onClick={() => setFlow({ type: "settle" })}>Registrar baixa integral</button>}
        <h3>Histórico de baixas e estornos</h3>
        {!data.history.totalElements && <p>Nenhuma baixa registrada.</p>}
        <div className="finance-history">{data.events.map((payment) => <article key={payment.id}>
          <strong>Baixa #{payment.id} · {money(payment.paidAmount)}</strong>
          <p>{dateLabel(payment.paidAt)} · {paymentLabels[payment.paymentMethod]} · Conta #{payment.bankAccountId} · Operador #{payment.userId}</p>
          {payment.notes && <p>{payment.notes}</p>}
          {payment.reversals.map((reversal) => <div key={reversal.id}><strong>Estorno #{reversal.id} · {dateLabel(reversal.reversedAt)}</strong><p>{reversal.reason} · Operador #{reversal.userId}</p></div>)}
          {has("finance.reverse") && data.title.status === "PAID" && data.history.totalElements === 1 && !payment.reversals.length &&
            <button className="secondary" onClick={() => setFlow({ type: "reverse", payment })}>Registrar estorno</button>}
        </article>)}</div>
        <Pagination page={page} totalPages={data.history.totalPages} change={setPage} />
        <p className="field-hint">Registros históricos não podem ser editados ou excluídos. Estornos exigem documento anterior ao despacho/recebimento e permitem apenas o cancelamento posterior.</p>
        {data.title.status === "PARTIAL" && <p>Este histórico parcial exige conciliação específica; não aceita novas baixas nesta versão.</p>}
      </>}
    </div>
    {flow && data && <FinancialEventForm title={data.title} purchase={purchase} payment={flow.payment} onClose={() => { setFlow(null); state.reload(); onChanged(); }} onSaved={changed} />}
  </Modal>;
}

function FinancialEventForm({ title, purchase, payment, onClose, onSaved }) {
  const { has } = useAuth();
  const reverse = Boolean(payment);
  const allowed = has(reverse ? "finance.reverse" : "finance.settle");
  const service = purchase ? payables : receivables;
  const action = useProductAction();
  const [bankAccountId, setBank] = useState("");
  const [date, setDate] = useState(localDate());
  const [method, setMethod] = useState(purchase ? "BANK_TRANSFER" : title.paymentMethod || "");
  const [notes, setNotes] = useState("");
  const [attempt, setAttempt] = useState(null);
  const banks = useProductResource((options) => bankAccounts.options({ active: true }, options), [], !reverse && allowed);
  if (!allowed) return null;
  const submit = (event) => {
    event.preventDefault();
    const body = attempt || (reverse ? { reversedAt: date, reason: notes.trim() } : {
      bankAccountId: Number(bankAccountId), amount: title.amount, paidAt: date, paymentMethod: method, notes: notes.trim() || null,
    });
    action.run(async () => {
      setAttempt(body);
      if (reverse) await service.reverse(title.id, payment.id, body);
      else await service.settle(title.id, body);
      onSaved();
    }, reverse ? `Estornar integralmente ${money(payment.paidAmount)}? O documento deverá ser cancelado depois.` : `Confirmar a baixa manual integral de ${money(title.amount)}?`, {
      title: reverse ? "Confirmar estorno" : "Confirmar baixa", confirmLabel: reverse ? "Registrar estorno" : "Registrar baixa", destructive: reverse,
    });
  };
  return <Modal title={reverse ? "Estorno manual integral" : "Baixa manual integral"} onClose={onClose} busy={action.pending}>
    <form className="finance-form" onSubmit={submit}>
      <ErrorAlert message={action.error} />
      {attempt && action.error && <p role="alert">O resultado pode ter sido registrado. Confira o histórico ao fechar ou repita exatamente os mesmos dados; não haverá uma segunda baixa.</p>}
      {!reverse && <ErrorAlert message={banks.error} retry={banks.reload} />}
      <p>Valor integral: <strong>{money(reverse ? payment.paidAmount : title.amount)}</strong></p>
      <fieldset disabled={action.pending || Boolean(attempt)}>
        {!reverse && <><FinanceSelect required label="Conta ativa" value={bankAccountId} empty="Selecione" values={Object.fromEntries((banks.data || []).map((b) => [b.id, b.name]))} onChange={(e) => setBank(e.target.value)} />
          <FinanceSelect required label="Método de pagamento" value={method} values={paymentLabels} empty="Selecione" disabled={!purchase} onChange={(e) => setMethod(e.target.value)} /></>}
        <label>{reverse ? "Data do estorno" : "Data da baixa"}<input required type="date" min={(reverse ? payment.paidAt : title.issuedAt)?.slice(0,10)} max={localDate()} value={date} onChange={(e) => setDate(e.target.value)} /></label>
        <label>{reverse ? "Motivo obrigatório" : "Observação"}<textarea required={reverse} maxLength={255} value={notes} onChange={(e) => setNotes(e.target.value)} /></label>
      </fieldset>
      <div className="actions"><button type="button" className="secondary" disabled={action.pending} onClick={onClose}>Fechar e conferir histórico</button>
        <button disabled={action.pending || (!reverse && (!banks.data?.length || banks.loading || !method))}>{action.pending ? "Registrando…" : attempt ? "Repetir os mesmos dados" : reverse ? "Registrar estorno" : "Registrar baixa"}</button></div>
    </form>
  </Modal>;
}
