import {
  receiptKey,
  pendingReceipt,
  savePendingReceipt,
  clearPendingReceipt,
  pendingBase,
} from "./purchaseReceiptStorage.js";
import { removeDraft, useDraft } from "../../auth/drafts";
import useDirtyUnload from "../../hooks/useDirtyUnload";
import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import EmbeddedContent from "../../components/layout/EmbeddedContent";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useConfirm from "../../components/feedback/useConfirm";
import { purchases } from "../../services/operations";
import { baseNumber, decimal, quantity } from "../Products/productFormData";

export default function PurchaseReceiptForm({
  purchase,
  items,
  onClose,
  onSaved,
  embedded = false,
  closeRef,
}) {
  const Container = embedded ? EmbeddedContent : Modal;
  const { has, session } = useAuth();
  const [initialAttempt] = useState(() => {
    try {
      return { attempt: pendingReceipt(session.userId, purchase.id), error: "" };
    } catch (issue) {
      return { attempt: null, error: issue.message };
    }
  });
  const finish = (receipt) => {
    removeDraft(`purchase-receipt-fields:${session.userId}:${purchase.id}:amounts`);
    removeDraft(`purchase-receipt-fields:${session.userId}:${purchase.id}:notes`);
    removeDraft(`purchase-receipt-fields:${session.userId}:${purchase.id}:modes`);
    onSaved(receipt);
  };
  const [frozen, setFrozen] = useState(initialAttempt.attempt);
  const [storageError, setStorageError] = useState(initialAttempt.error);
  const draftKey = `purchase-receipt-fields:${session.userId}:${purchase.id}`;
  const [amounts, setAmounts] = useDraft(
    `${draftKey}:amounts`,
    Object.fromEntries(items.map((item) => [item.id, ""])),
  );
  const [modes, setModes] = useDraft(
    `${draftKey}:modes`,
    Object.fromEntries(
      items
        .filter((item) => String(amounts[item.id] || "").trim())
        .map((item) => [item.id, "base"]),
    ),
  );
  const [invalidItem, setInvalidItem] = useState(null);
  const [notes, setNotes] = useDraft(`${draftKey}:notes`, "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useDirtyUnload(busy || !!notes || Object.values(amounts).some(Boolean));
  useEffect(() => {
    if (invalidItem && error) document.getElementById(`receipt-amount-${invalidItem}`)?.focus();
  }, [invalidItem, error]);
  const formRef = useRef(null);
  useEffect(() => {
    formRef.current?.scrollIntoView({ block: "start" });
    formRef.current?.querySelector("input:not(:disabled)")?.focus({ preventScroll: true });
  }, []);
  const locked = useRef(false);
  const confirm = useConfirm();
  const lockName = `${receiptKey(session.userId, purchase.id)}:operation`;
  const withPurchaseLock = async (action) => {
    if (!globalThis.navigator?.locks?.request) {
      throw new Error(
        "Este navegador não oferece bloqueio seguro entre abas. Use um navegador compatível para registrar recebimentos.",
      );
    }
    return navigator.locks.request(
      lockName,
      { mode: "exclusive", ifAvailable: true },
      async (lock) => {
        if (!lock) {
          throw new Error(
            "Há outra aba processando esta compra. Aguarde a conclusão e consulte novamente a tentativa antes de enviar.",
          );
        }
        return action();
      },
    );
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
    removeDraft(`${draftKey}:amounts`);
    removeDraft(`${draftKey}:notes`);
    removeDraft(`${draftKey}:modes`);
    onClose();
  };
  useEffect(() => {
    if (closeRef) closeRef.current = close;
    return () => {
      if (closeRef) closeRef.current = null;
    };
  });
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
          setError(
            "Outra aba criou uma tentativa para esta compra. Revise os dados recuperados antes de prosseguir.",
          );
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
          finish(registered);
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
          setError(
            "Existe uma tentativa criada ou alterada em outra aba. Revise-a e confirme antes de repetir.",
          );
          return;
        }
        let payload = current || frozen;
        if (frozen && !current) {
          // Another tab may have completed and removed this attempt. Do not send blindly.
          const receipts = await purchases.receipts(purchase.id);
          const registered = receipts.find((receipt) => receipt.requestId === frozen.requestId);
          if (registered) {
            finish(registered);
          } else {
            setError(
              "A tentativa desta aba não está mais salva. Verifique o histórico e recarregue os dados antes de registrar outro recebimento.",
            );
          }
          return;
        }
        if (!payload) {
          const lines = items
            .filter((item) => String(amounts[item.id] ?? "").trim() !== "")
            .map((item) => {
              setInvalidItem(item.id);
              const baseMode = modes[item.id] === "base";
              const entered = baseMode
                ? baseNumber(
                    amounts[item.id],
                    item.unitSnapshot || item.product.unit,
                    `Item #${item.id}: quantidade BASE`,
                    false,
                    true,
                  )
                : decimal(amounts[item.id], `Item #${item.id}: quantidade de apresentações`, {
                    positive: true,
                    places: 0,
                    max: 9999999999999.9,
                  });
              const base = receiptBase(item, entered, baseMode);
              if (base > scaledReceipt(pendingBase(item)))
                throw new Error(
                  `Item #${item.id}: a quantidade informada supera o saldo pendente. Ajuste o recebimento deste item.`,
                );
              return {
                purchaseItemId: item.id,
                [baseMode ? "quantity" : "commercialQuantity"]: entered,
              };
            });
          setInvalidItem(null);
          if (!lines.length)
            throw new Error("Informe a quantidade que chegou em pelo menos um item.");
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
          finish(receipt);
        } catch (issue) {
          const rejectedByApi =
            [400, 422].includes(issue.status) &&
            issue.data?.status === issue.status &&
            typeof issue.data?.message === "string";
          if (rejectedByApi) {
            try {
              clearMatchingAttempt(payload.requestId);
            } catch {
              setStorageError(
                "Não foi possível limpar a tentativa rejeitada. Verifique o armazenamento do navegador antes de reenviar.",
              );
              setError(issue.message);
              return;
            }
            setFrozen(null);
            setInvalidItem(Number(/Item #(\d+)/.exec(issue.message)?.[1]) || null);
            setAmounts(
              Object.fromEntries(
                items.map((item) => [
                  item.id,
                  String(
                    payload.items.find((line) => line.purchaseItemId === item.id)
                      ?.commercialQuantity ??
                      payload.items.find((line) => line.purchaseItemId === item.id)?.quantity ??
                      "",
                  ).replace(".", ","),
                ]),
              ),
            );
            setModes(
              Object.fromEntries(
                payload.items.map((line) => [
                  line.purchaseItemId,
                  line.commercialQuantity != null ? "commercial" : "base",
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
      if (
        issue.name === "SecurityError" ||
        issue.name === "QuotaExceededError" ||
        issue.message ===
          "Não foi possível acessar a tentativa de recebimento salva neste navegador."
      ) {
        setStorageError(
          "Não foi possível acessar ou salvar a tentativa. O recebimento foi bloqueado por segurança.",
        );
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  return (
    <Container
      title={`Receber compra #${purchase.id}`}
      onClose={close}
      busy={busy}
      wide
      className="purchase-dialog"
    >
      <form ref={formRef} className="customer-form purchase-receipt-form" onSubmit={submit}>
        <ErrorAlert message={storageError || error} />
        {frozen ? (
          <>
            <p className="product-warning">
              Existe uma tentativa aguardando confirmação. Repetir envia exatamente o mesmo
              identificador e as mesmas quantidades, protegendo contra entrada duplicada. Você pode
              fechar esta aba e retomar a tentativa ao entrar novamente com o mesmo usuário neste
              navegador.
            </p>
            <dl className="customer-details">
              <div>
                <dt>Identificador da tentativa</dt>
                <dd>{frozen.requestId}</dd>
              </div>
            </dl>
            {frozen.items.map((line) => (
              <p className="purchase-receipt-attempt" key={line.purchaseItemId}>
                Item #{line.purchaseItemId}: {quantity(line.commercialQuantity ?? line.quantity)}{" "}
                {line.commercialQuantity != null ? "apresentação(ões) contratada(s)" : "BASE"}
              </p>
            ))}
            <p>{frozen.notes}</p>
          </>
        ) : (
          <fieldset disabled={busy || !has("purchases.receive")} className="customer-form-fields">
            <p className="field-hint">
              Informe o que chegou agora na apresentação contratada (quantidade inteira), ou escolha
              BASE para quantidade avulsa. Itens vazios continuam pendentes.
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
                <select
                  aria-label={`Unidade de recebimento do item ${item.id}`}
                  value={modes[item.id] || "commercial"}
                  onChange={(e) => {
                    setModes((old) => ({ ...old, [item.id]: e.target.value }));
                    setAmounts((old) => ({ ...old, [item.id]: "" }));
                  }}
                >
                  <option value="commercial">
                    {item.presentationNameSnapshot || "Apresentação contratada"} (inteiras)
                  </option>
                  <option value="base">
                    BASE · {item.unitSnapshot || item.product.unit} (avulsa)
                  </option>
                </select>
                <input
                  id={`receipt-amount-${item.id}`}
                  aria-invalid={invalidItem === item.id}
                  placeholder={
                    modes[item.id] === "base"
                      ? "Receber agora em BASE"
                      : "Apresentações recebidas agora"
                  }
                  aria-label={`Quantidade recebida agora do item ${item.id}`}
                  inputMode="decimal"
                  value={amounts[item.id] ?? ""}
                  disabled={pendingBase(item) === "0" || item.baseQuantity == null}
                  onChange={(event) =>
                    setAmounts((old) => ({ ...old, [item.id]: event.target.value }))
                  }
                />
                <ReceiptConversion
                  item={item}
                  amount={amounts[item.id]}
                  baseMode={modes[item.id] === "base"}
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
    </Container>
  );
}

// Quantities have at most one decimal in BASE. Keep conversion exact in the preview.
function scaledReceipt(value) {
  const raw = String(value ?? "").replace(",", ".");
  if (!/^\d+(?:\.\d)?$/.test(raw))
    throw new Error("Conversão BASE indisponível. Confira a quantidade e a unidade do item.");
  const [whole, fraction = ""] = raw.split(".");
  return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
}
function receiptBase(item, entered, baseMode) {
  if (baseMode) return scaledReceipt(entered);
  if (item.baseFactorSnapshot == null)
    throw new Error(
      `Item #${item.id}: conversão contratada indisponível. Confirme a conversão antes de receber.`,
    );
  return BigInt(entered) * scaledReceipt(item.baseFactorSnapshot);
}
function ReceiptConversion({ item, amount, baseMode }) {
  if (!String(amount || "").trim())
    return (
      <small>
        1 {item.presentationNameSnapshot || "apresentação"} = {quantity(item.baseFactorSnapshot)}{" "}
        {item.unitSnapshot || "BASE"}.
      </small>
    );
  try {
    const entered = baseMode
      ? baseNumber(amount, item.unitSnapshot || item.product.unit, "Quantidade", false, true)
      : decimal(amount, "Apresentações", { positive: true, places: 0, max: 9999999999999.9 });
    const base = receiptBase(item, entered, baseMode);
    const remaining = scaledReceipt(pendingBase(item)) - base;
    const display = (value) => `${value / 10n}${value % 10n ? "," + String(value % 10n) : ""}`;
    return (
      <small className={remaining < 0n ? "error-alert" : ""}>
        Receber agora: {display(base)} {item.unitSnapshot || "BASE"} ·{" "}
        {remaining < 0n ? "Acima do saldo pendente" : `Restará: ${display(remaining)} BASE`}
      </small>
    );
  } catch (issue) {
    return <small className="error-alert">{issue.message}</small>;
  }
}
