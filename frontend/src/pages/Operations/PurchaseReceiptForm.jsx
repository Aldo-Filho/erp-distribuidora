import { useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useConfirm from "../../components/feedback/useConfirm";
import { purchases } from "../../services/operations";
import { baseNumber, quantity } from "../Products/productFormData";

export const receiptKey = (userId, purchaseId) => `jamshub:receipt:${userId}:${purchaseId}`;

// Keep the exact request across tab closures. Keys are scoped to the authenticated user and purchase.
export function pendingReceipt(userId, purchaseId) {
  const key = receiptKey(userId, purchaseId);
  try {
    const saved = localStorage.getItem(key);
    if (saved) return JSON.parse(saved);
    // Preserve attempts created before persistent storage was introduced.
    const previous = sessionStorage.getItem(key);
    if (!previous) return null;
    const attempt = JSON.parse(previous);
    localStorage.setItem(key, previous);
    sessionStorage.removeItem(key);
    return attempt;
  } catch {
    // Never silently discard an uncertain attempt or generate a new ID on storage failure.
    throw new Error("Não foi possível acessar a tentativa de recebimento salva neste navegador.");
  }
}

export function savePendingReceipt(userId, purchaseId, attempt) {
  localStorage.setItem(receiptKey(userId, purchaseId), JSON.stringify(attempt));
}

export function clearPendingReceipt(userId, purchaseId) {
  const key = receiptKey(userId, purchaseId);
  localStorage.removeItem(key);
  sessionStorage.removeItem(key);
}
// Display-only subtraction, without converting a commercial amount in the browser.
export function pendingBase(item) {
  if (item.baseQuantity == null || item.receivedQuantity == null) return null;
  const scaled = (value) => {
    const [whole, fraction = ""] = String(value).split(".");
    return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
  };
  const difference = scaled(item.baseQuantity) - scaled(item.receivedQuantity);
  return `${difference / 10n}${difference % 10n ? "," + String(difference % 10n) : ""}`;
}
export default function PurchaseReceiptForm({ purchase, items, onClose, onSaved }) {
  const { has, session } = useAuth();
  const [initialAttempt] = useState(() => {
    try {
      return { attempt: pendingReceipt(session.userId, purchase.id), error: "" };
    } catch (issue) {
      return { attempt: null, error: issue.message };
    }
  });
  const [frozen, setFrozen] = useState(initialAttempt.attempt);
  const [storageError, setStorageError] = useState(initialAttempt.error);
  const [amounts, setAmounts] = useState(() =>
    Object.fromEntries(items.map((item) => [item.id, ""])),
  );
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const locked = useRef(false);
  const confirm = useConfirm();
  const lockName = `${receiptKey(session.userId, purchase.id)}:operation`;
  const withPurchaseLock = async (action) => {
    if (!globalThis.navigator?.locks?.request) {
      throw new Error("Este navegador não oferece bloqueio seguro entre abas. Use um navegador compatível para registrar recebimentos.");
    }
    return navigator.locks.request(lockName, { mode: "exclusive", ifAvailable: true }, async (lock) => {
      if (!lock) {
        throw new Error("Há outra aba processando esta compra. Aguarde a conclusão e consulte novamente a tentativa antes de enviar.");
      }
      return action();
    });
  };
  const clearMatchingAttempt = (requestId) => {
    const current = pendingReceipt(session.userId, purchase.id);
    if (current && current.requestId !== requestId) {
      return;
    }
    clearPendingReceipt(session.userId, purchase.id);
  };
  const close = async () => {
    if (locked.current) return;
    if (
      !frozen &&
      (notes || Object.values(amounts).some(Boolean)) &&
      !(await confirm("Descartar as quantidades ainda não enviadas?", {
        title: "Descartar recebimento",
        confirmLabel: "Descartar",
        destructive: true,
      }))
    )
      return;
    onClose();
  };
  const verifyReceipt = async () => {
    if (locked.current || !frozen) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await withPurchaseLock(async () => {
        const current = pendingReceipt(session.userId, purchase.id);
        if (current && current.requestId !== frozen.requestId) {
          setFrozen(current);
          setError("Outra aba criou uma tentativa para esta compra. Revise os dados recuperados antes de prosseguir.");
          return;
        }
        const receipts = await purchases.receipts(purchase.id);
        const registered = receipts.find((receipt) => receipt.requestId === frozen.requestId);
        if (registered) {
          try {
            clearMatchingAttempt(frozen.requestId);
          } catch {
            // The server history already confirms the receipt.
          }
          onSaved(registered);
        } else {
          setError(
            "Ainda não foi encontrado um recibo para esta tentativa. Se o envio anterior já terminou, você pode repetir a mesma tentativa sem gerar outro identificador.",
          );
        }
      });
    } catch (issue) {
      setError(`${issue.message} A tentativa foi preservada para nova consulta ou repetição.`);
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  const submit = async (event) => {
    event.preventDefault();
    if (locked.current || storageError || !has("purchases.receive")) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await withPurchaseLock(async () => {
        // Read again under the cross-tab lock; the state seen on render may be outdated.
        const current = pendingReceipt(session.userId, purchase.id);
        if (current && (!frozen || current.requestId !== frozen.requestId)) {
          setFrozen(current);
          setError("Existe uma tentativa criada ou alterada em outra aba. Revise-a e confirme antes de repetir.");
          return;
        }
        let payload = current || frozen;
        if (frozen && !current) {
          // Another tab may have completed and removed this attempt. Do not send blindly.
          const receipts = await purchases.receipts(purchase.id);
          const registered = receipts.find((receipt) => receipt.requestId === frozen.requestId);
          if (registered) {
            onSaved(registered);
          } else {
            setError("A tentativa desta aba não está mais salva. Verifique o histórico e recarregue os dados antes de registrar outro recebimento.");
          }
          return;
        }
        if (!payload) {
          const lines = items
            .filter((item) => String(amounts[item.id]).trim() !== "")
            .map((item) => ({
              purchaseItemId: item.id,
              quantity: baseNumber(
                amounts[item.id],
                item.unitSnapshot || item.product.unit,
                `Quantidade do item #${item.id}`,
                false,
                true,
              ),
            }));
          if (!lines.length) throw new Error("Informe ao menos uma quantidade recebida em BASE.");
          payload = {
            requestId: crypto.randomUUID(),
            operatorUserId: session.userId,
            notes: notes.trim() || null,
            items: lines,
          };
          // Persist the exact attempt before sending; this remains inside the cross-tab lock.
          savePendingReceipt(session.userId, purchase.id, payload);
          setFrozen(payload);
        }
        try {
          const receipt = await purchases.receive(purchase.id, payload);
          // The API confirmation is independent of local cleanup.
          try {
            clearMatchingAttempt(payload.requestId);
          } catch {
            // A later reconciliation can clear the leftover attempt.
          }
          onSaved(receipt);
        } catch (issue) {
          const rejectedByApi =
            [400, 422].includes(issue.status) &&
            issue.data?.status === issue.status &&
            typeof issue.data?.message === "string";
          if (rejectedByApi) {
            try {
              clearMatchingAttempt(payload.requestId);
            } catch {
              setStorageError("Não foi possível limpar a tentativa rejeitada. Verifique o armazenamento do navegador antes de reenviar.");
              setError(issue.message);
              return;
            }
            setFrozen(null);
            setAmounts(
              Object.fromEntries(
                items.map((item) => [
                  item.id,
                  String(payload.items.find((line) => line.purchaseItemId === item.id)?.quantity ?? "").replace(".", ","),
                ]),
              ),
            );
            setNotes(payload.notes || "");
          }
          setError(
            !rejectedByApi
              ? `${issue.message} Não foi possível confirmar o resultado. Repita a mesma tentativa antes de registrar outro recebimento.`
              : issue.message,
          );
        }
      });
    } catch (issue) {
      setError(issue.message);
      // Storage errors must prevent a new attempt rather than allow an unsafe retry.
      if (issue.name === "SecurityError" || issue.name === "QuotaExceededError" ||
          issue.message === "Não foi possível acessar a tentativa de recebimento salva neste navegador.") {
        setStorageError("Não foi possível acessar ou salvar a tentativa. O recebimento foi bloqueado por segurança.");
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  return (
    <Modal
      title={`Receber compra #${purchase.id} · BASE`}
      onClose={close}
      busy={busy}
      wide
      className="purchase-dialog"
    >
      <form className="customer-form purchase-receipt-form" onSubmit={submit}>
        <ErrorAlert message={storageError || error} />
        {frozen ? (
          <>
            <p className="product-warning">
              Existe uma tentativa aguardando confirmação. Repetir envia exatamente o mesmo
              identificador e as mesmas quantidades, protegendo contra entrada duplicada. Você pode
              fechar esta aba e retomar a tentativa ao entrar novamente com o mesmo usuário neste navegador.
            </p>
            <dl className="customer-details">
              <div>
                <dt>Identificador da tentativa</dt>
                <dd>{frozen.requestId}</dd>
              </div>
            </dl>
            {frozen.items.map((line) => (
              <p className="purchase-receipt-attempt" key={line.purchaseItemId}>
                Item #{line.purchaseItemId}: {quantity(line.quantity)} BASE
              </p>
            ))}
            <p>{frozen.notes}</p>
          </>
        ) : (
          <fieldset disabled={busy || !has("purchases.receive")} className="customer-form-fields">
            <p className="field-hint">
              Informe somente o que chegou agora em unidade BASE. Deixe vazios os itens que não
              chegaram; o restante continuará pendente.
            </p>
            {items.map((item) => (
              <label className="purchase-receipt-line" key={item.id}>
                <strong>
                  #{item.id} · {item.skuSnapshot || item.product.sku} ·{" "}
                  {item.productNameSnapshot || item.product.name}
                </strong>
                <span>
                  {item.warehouse.name} ·{" "}
                  {item.presentationNameSnapshot || "Conversão legada pendente"}
                </span>
                <span>
                  Contratado: {quantity(item.baseQuantity)} · recebido:{" "}
                  {quantity(item.receivedQuantity)} · pendente:{" "}
                  {pendingBase(item) ?? "Conversão pendente"}{" "}
                  {item.unitSnapshot || item.product.unit}
                </span>
                <input
                  placeholder="Receber agora em BASE"
                  aria-label={`Receber item ${item.id} em BASE`}
                  inputMode="decimal"
                  value={amounts[item.id]}
                  disabled={pendingBase(item) === "0" || item.baseQuantity == null}
                  onChange={(event) =>
                    setAmounts((old) => ({ ...old, [item.id]: event.target.value }))
                  }
                />
              </label>
            ))}
            <label>
              Observação
              <textarea rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} />
            </label>
          </fieldset>
        )}
        <div className="actions">
          <button type="button" className="secondary" disabled={busy} onClick={close}>
            {frozen ? "Fechar e retomar depois" : "Cancelar"}
          </button>
          {frozen && (
            <button type="button" className="secondary" disabled={busy} onClick={verifyReceipt}>
              {busy ? "Consultando…" : "Verificar no histórico"}
            </button>
          )}
          <button disabled={busy || Boolean(storageError) || !has("purchases.receive")}>
            {busy ? "Registrando…" : frozen ? "Repetir a mesma tentativa" : "Registrar recebimento"}
          </button>
        </div>
      </form>
    </Modal>
  );
}