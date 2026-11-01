import { useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useConfirm from "../../components/feedback/useConfirm";
import { products } from "../../services/products";
import { stockOperations } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductForm from "../Products/useProductForm";
import { baseNumber, quantity, textNumber } from "../Products/productFormData";
import { SkuPicker, skuName, WarehouseSelect } from "./OperationSelectors";
import "./stock.css";

export function StockConfiguration({ stock, product: fixedProduct, onSaved, onClose }) {
  const { has } = useAuth();
  const form = useProductForm(
    `school:stock:${stock?.id || (fixedProduct ? "new:" + fixedProduct.id : "new")}`,
    {
      productId: String(stock?.product.id || fixedProduct?.id || ""),
      warehouseId: String(stock?.warehouse.id || ""),
      minQuantity: textNumber(stock?.minQuantity ?? 0),
      maxQuantity: textNumber(stock?.maxQuantity),
    },
    onClose,
    onSaved,
  );
  const [picker, setPicker] = useState(false);
  const item = useProductResource(
    (options) => products.findItem(form.value.productId, options),
    [form.value.productId],
    !fixedProduct && Boolean(form.value.productId) && has("products.read"),
  );
  const product = stock?.product || fixedProduct || item.data;
  const submit = (e) =>
    form.submit(e, async (value) => {
      if (!product?.unit || String(product.id) !== value.productId)
        throw new Error("Confirme a unidade do produto em Produtos antes de configurar o estoque.");
      const body = {
        minQuantity: baseNumber(value.minQuantity, product.unit, "Mínimo"),
        maxQuantity: baseNumber(value.maxQuantity, product.unit, "Máximo", true),
      };
      if (stock)
        return stockOperations.update(
          stock.id,
          body.maxQuantity == null
            ? { minQuantity: body.minQuantity, removeMaxQuantity: true }
            : body,
        );
      if (!value.productId || !value.warehouseId) throw new Error("Selecione produto e depósito.");
      return stockOperations.configure({
        ...body,
        productId: Number(value.productId),
        warehouseId: Number(value.warehouseId),
        quantity: 0,
        reservedQuantity: 0,
      });
    });
  return (
    <Modal
      title={stock ? "Configurar limites do estoque" : "Configurar produto no depósito"}
      onClose={form.close}
      busy={form.saving}
      wide
      className="stock-dialog"
    >
      <form className="customer-form" onSubmit={submit}>
        <ErrorAlert message={form.error} />
        <fieldset
          disabled={form.saving || !has("stock.configure")}
          className="customer-form-fields"
        >
          <div className="form-grid">
            <label className="span-all">
              Produto / SKU
              <div className="stock-product-control">
                <input
                  readOnly
                  value={product ? skuName(product) : ""}
                  placeholder="Selecione um produto / SKU"
                />
                {!stock && !fixedProduct && (
                  <button
                    type="button"
                    className="secondary"
                    disabled={!has("products.read")}
                    onClick={() => setPicker(true)}
                  >
                    {product ? "Trocar produto" : "Selecionar produto"}
                  </button>
                )}
              </div>
            </label>
            {!stock && (
              <div className="span-all">
                <WarehouseSelect
                  value={form.value.warehouseId}
                  onChange={(e) => form.change("warehouseId", e.target.value)}
                />
              </div>
            )}
            {stock && (
              <label className="span-all">
                Depósito
                <input readOnly value={stock.warehouse.name} />
              </label>
            )}
            {!stock && !fixedProduct && form.value.productId && (
              <div className="span-all">
                <ProductLoad state={item}>{null}</ProductLoad>
              </div>
            )}
            <p className="field-hint span-all">
              Quantidades em BASE ({product?.unit || "unidade pendente"}).{" "}
              {stock
                ? "A configuração altera somente os limites. Use Movimentar estoque para alterar o saldo."
                : "O novo item começa com saldo e reserva zero. Use Movimentar estoque para alterar o saldo."}
            </p>
            <label>
              Mínimo
              <input
                required
                inputMode="decimal"
                value={form.value.minQuantity}
                onChange={(e) => form.change("minQuantity", e.target.value)}
              />
            </label>
            <label>
              Máximo (opcional)
              <input
                inputMode="decimal"
                value={form.value.maxQuantity}
                onChange={(e) => form.change("maxQuantity", e.target.value)}
              />
            </label>
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            Cancelar
          </button>
          <button disabled={form.saving || !has("stock.configure")}>
            {form.saving ? "Salvando…" : "Salvar configuração"}
          </button>
        </div>
      </form>
      {picker && (
        <SkuPicker
          onClose={() => setPicker(false)}
          onSelect={(product) => {
            form.change("productId", String(product.id));
            setPicker(false);
          }}
        />
      )}
    </Modal>
  );
}
//* A movement attempt keeps its exact request body until the server confirms the result.*
const movementKey = (userId, stockItemId) => `jamshub:stock-movement:${userId}:${stockItemId}`;
const movementLabels = {
  INBOUND: "Entrada manual",
  OUTBOUND: "Saída manual",
  CORRECTION: "Corrigir saldo",
  ADJUSTMENT: "Ajuste de saldo",
};

// Integer tenths keep both the correction payload and its preview exact.
function scaledQuantity(value) {
  const text = String(value);
  const [whole, fraction = ""] = text.replace(/^-/, "").split(".");
  const scaled = BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
  return text.startsWith("-") ? -scaled : scaled;
}

function movementPreview(value, stock) {
  const correction = value.type === "CORRECTION";
  if (!["INBOUND", "OUTBOUND", "CORRECTION"].includes(value.type)) {
    throw new Error("Selecione uma operação válida.");
  }
  const amount = baseNumber(
    value.quantity,
    stock.product.unit,
    correction ? "Quantidade contada" : "Quantidade",
    false,
    !correction,
  );
  const current = scaledQuantity(stock.quantity);
  const entered = scaledQuantity(amount);
  let difference = entered;
  if (correction) {
    difference = entered - current;
  } else if (value.type === "OUTBOUND") {
    difference = -entered;
  }
  const next = current + difference;
  if (next < 0n) throw new Error("A quantidade de saída supera o estoque atual.");
  if (next < scaledQuantity(stock.reservedQuantity)) {
    throw new Error("O saldo após a operação não pode ser menor que a quantidade reservada.");
  }
  baseNumber(String(Number(next) / 10), stock.product.unit, "Saldo após a operação");
  return { amount, difference: Number(difference) / 10, next: Number(next) / 10 };
}

function readMovement(userId, stockItemId) {
  try {
    const saved = localStorage.getItem(movementKey(userId, stockItemId));
    if (!saved) return null;
    const attempt = JSON.parse(saved);
    if (
      !attempt || typeof attempt.requestId !== "string" ||
      attempt.stockItemId !== stockItemId || !["INBOUND", "OUTBOUND", "ADJUSTMENT"].includes(attempt.type) ||
      attempt.reason !== "MANUAL_ADJUSTMENT" || typeof attempt.quantity !== "number" ||
      (attempt.expectedQuantity != null &&
        (attempt.type !== "ADJUSTMENT" || typeof attempt.expectedQuantity !== "number"))
    ) {
      throw new Error("Invalid saved stock movement.");
    }
    return attempt;
  } catch {
    throw new Error("Não foi possível ler a tentativa de estoque salva. O envio foi bloqueado por segurança.");
  }
}
function clearMovement(userId, stockItemId, requestId) {
  const current = readMovement(userId, stockItemId);
  if (current?.requestId === requestId) {
    localStorage.removeItem(movementKey(userId, stockItemId));
  }
}
export function StockMovement({ stock, onSaved, onClose }) {
  const { has, session } = useAuth();
  const confirm = useConfirm();
  const initial = useRef(null);
  if (initial.current === null) {
    try {
      initial.current = { attempt: readMovement(session.userId, stock.id), error: "" };
    } catch (issue) {
      initial.current = { attempt: null, error: issue.message };
    }
  }
  const [frozen, setFrozen] = useState(initial.current.attempt);
  const [storageError, setStorageError] = useState(initial.current.error);
  const [value, setValue] = useState({ type: "INBOUND", quantity: "", notes: "" });
  const [currentStock, setCurrentStock] = useState(stock);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [rejectedRequestId, setRejectedRequestId] = useState(null);
  const locked = useRef(false);
  const key = movementKey(session.userId, stock.id);
  let preview = null;
  let previewError = "";
  if (!frozen && value.quantity.trim()) {
    try {
      preview = movementPreview(value, currentStock);
    } catch (issue) {
      previewError = issue.message;
    }
  }
  const close = async () => {
    if (locked.current) return;
    if (!frozen && (value.quantity.trim() || value.notes.trim()) &&
        !(await confirm("Descartar o movimento ainda não enviado?", {
          title: "Descartar movimento", confirmLabel: "Descartar", destructive: true,
        }))) return;
    onClose();
  };
  const withLock = async (action) => {
    if (!globalThis.navigator?.locks?.request) {
      throw new Error("Este navegador não oferece bloqueio seguro entre abas para movimentos de estoque.");
    }
    return navigator.locks.request(`${key}:operation`, { mode: "exclusive", ifAvailable: true }, (lock) => {
      if (!lock) throw new Error("Outra aba está movimentando este item. Aguarde e tente novamente.");
      return action();
    });
  };
  const verifyMovement = async () => {
    if (locked.current || !frozen) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await withLock(async () => {
        const saved = readMovement(session.userId, stock.id);
        if (!saved) {
          setError("A tentativa não está mais salva neste navegador. Consulte o histórico antes de registrar outro movimento.");
          return;
        }
        if (saved.requestId !== frozen.requestId) {
          setFrozen(saved);
          setError("Outra aba atualizou a tentativa deste item. Confira os dados antes de continuar.");
          return;
        }
        const movements = await stockOperations.history({ stockItemId: stock.id });
        const registered = movements.find((movement) => movement.requestId === saved.requestId);
        if (!registered) {
          setError("A tentativa ainda não aparece no histórico. Se o envio anterior terminou, repita o mesmo identificador e os mesmos dados.");
          return;
        }
        try {
          clearMovement(session.userId, stock.id, saved.requestId);
        } catch {
          //* The history confirms the operation, even if local cleanup fails.*
        }
        onSaved(registered);
      });
    } catch (issue) {
      setError(`${issue.message} A tentativa foi preservada para nova consulta ou repetição.`);
      if (issue.name === "SecurityError" || issue.message?.includes("tentativa de estoque salva")) {
        setStorageError("Não foi possível acessar a tentativa. O envio foi bloqueado por segurança.");
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  const discardRejected = async () => {
    if (locked.current || storageError || !frozen || rejectedRequestId !== frozen.requestId) return;
    if (!(await confirm(
      "Descartar a tentativa rejeitada? Antes de permitir uma nova tentativa, o sistema consultará o histórico. A ausência no histórico não garante que uma operação ainda em processamento não seja registrada.",
      { title: "Descartar tentativa rejeitada", confirmLabel: "Verificar e descartar", destructive: true },
    ))) return;
    if (locked.current) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await withLock(async () => {
        const saved = readMovement(session.userId, stock.id);
        if (!saved || saved.requestId !== frozen.requestId) {
          setError("A tentativa salva mudou ou não está disponível. Reabra o formulário antes de continuar.");
          return;
        }
        const movements = await stockOperations.history({ stockItemId: stock.id });
        const registered = movements.find((movement) => movement.requestId === saved.requestId);
        if (registered) {
          try {
            clearMovement(session.userId, stock.id, saved.requestId);
          } catch {
            // A confirmação do servidor prevalece sobre a limpeza local.
          }
          onSaved(registered);
          return;
        }
        const latest = await stockOperations.search({
          productId: stock.product.id,
          warehouseId: stock.warehouse.id,
          size: 100,
        });
        const refreshedStock = latest.content.find((entry) => entry.id === stock.id);
        if (!refreshedStock) throw new Error("Estoque não encontrado. Reabra a consulta.");
        clearMovement(session.userId, stock.id, saved.requestId);
        setFrozen(null);
        setRejectedRequestId(null);
        setCurrentStock(refreshedStock);
        const correction = saved.type === "ADJUSTMENT";
        const entered = correction
          ? Number(scaledQuantity(saved.expectedQuantity ?? refreshedStock.quantity) + scaledQuantity(saved.quantity)) / 10
          : saved.quantity;
        setValue({
          type: correction ? "CORRECTION" : saved.type,
          quantity: textNumber(entered),
          notes: saved.notes || "",
        });
        setError("A tentativa rejeitada foi descartada após consulta ao histórico. Corrija os dados antes de um novo envio.");
      });
    } catch (issue) {
      setError(`${issue.message} A tentativa foi mantida; não foi possível confirmar que é seguro descartá-la.`);
      if (issue.name === "SecurityError" || issue.message?.includes("tentativa de estoque salva")) {
        setStorageError("Não foi possível acessar a tentativa. O envio foi bloqueado por segurança.");
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    if (locked.current || storageError || !has("stock.adjust")) return;
    locked.current = true;
    setBusy(true);
    setError("");
    try {
      await withLock(async () => {
        const saved = readMovement(session.userId, stock.id);
        if (saved && (!frozen || saved.requestId !== frozen.requestId)) {
          setFrozen(saved);
          setRejectedRequestId(null);
          setError("Outra aba já salvou uma tentativa para este item. Revise-a antes de repetir.");
          return;
        }
        if (frozen && !saved) {
          setError("A tentativa desta aba não está mais salva. Consulte o histórico antes de registrar outro movimento.");
          return;
        }
        let payload = saved;
        if (!payload) {
          const movement = movementPreview(value, currentStock);
          const correction = value.type === "CORRECTION";
          if (correction && movement.difference === 0) {
            throw new Error("A quantidade contada já corresponde ao estoque atual. Não há correção a registrar.");
          }
          payload = {
            requestId: crypto.randomUUID(),
            stockItemId: stock.id,
            type: correction ? "ADJUSTMENT" : value.type,
            reason: "MANUAL_ADJUSTMENT",
            quantity: correction ? movement.difference : movement.amount,
            userId: session.userId,
            notes: value.notes.trim() || null,
          };
          if (correction) payload.expectedQuantity = Number(currentStock.quantity);
          localStorage.setItem(key, JSON.stringify(payload));
          setFrozen(payload);
        }
        try {
          const movement = await stockOperations.move(payload);
          try {
            clearMovement(session.userId, stock.id, payload.requestId);
          } catch {
            //* The API confirmed the operation; a later visit can clear the local copy.*
          }
          onSaved(movement);
        } catch (issue) {
          const rejected = [400, 422].includes(issue.status) &&
            issue.data?.status === issue.status && typeof issue.data?.message === "string";
          const message = issue.data?.message === "Stock balance changed. Refresh the balance before correcting it."
            ? "O saldo mudou desde a consulta. Descarte a tentativa rejeitada para atualizar o saldo e revisar a correção."
            : issue.message;
          if (rejected) {
            setRejectedRequestId(payload.requestId);
          } else {
            setRejectedRequestId(null);
          }
          setError(rejected
            ? `${message} A tentativa foi preservada. Verifique no histórico ou descarte explicitamente a tentativa rejeitada para corrigir os dados.`
            : `${message} Resultado não confirmado. Repita a mesma tentativa, sem alterar o identificador.`);
        }
      });
    } catch (issue) {
      setError(issue.message);
      if (issue.name === "SecurityError" || issue.name === "QuotaExceededError" ||
          issue.message.includes("tentativa de estoque salva")) {
        setStorageError("Não foi possível acessar ou salvar a tentativa. O envio foi bloqueado por segurança.");
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  };
  return (
    <Modal title="Movimentar estoque" onClose={close} busy={busy} wide className="stock-dialog">
      <div className="customer-detail-title"><h3>{stock.product.name}</h3></div>
      <dl className="customer-details">
        <div><dt>SKU</dt><dd>{stock.product.sku}</dd></div>
        <div><dt>Depósito</dt><dd>{stock.warehouse.name}</dd></div>
        <div><dt>Unidade BASE</dt><dd>{stock.product.unit || "Unidade pendente"}</dd></div>
      </dl>
      <form className="customer-form stock-movement-form" onSubmit={submit}>
        <ErrorAlert message={storageError || error} />
        {frozen ? (
          <>
            <p className="product-warning">
              Existe uma movimentação aguardando confirmação. Repetir preserva exatamente o mesmo
              identificador e os mesmos dados, mesmo após fechar o navegador.
            </p>
            <dl className="customer-details">
              <div><dt>Identificador</dt><dd>{frozen.requestId}</dd></div>
              <div>
                <dt>Operação</dt>
                <dd>{frozen.expectedQuantity != null ? "Corrigir saldo" : movementLabels[frozen.type]}</dd>
              </div>
              <div>
                <dt>{frozen.type === "ADJUSTMENT" ? "Diferença a aplicar" : "Quantidade"}</dt>
                <dd>{quantity(frozen.quantity)} {stock.product.unit || "BASE"}</dd>
              </div>
              {frozen.expectedQuantity != null && (
                <>
                  <div><dt>Saldo usado no cálculo</dt><dd>{quantity(frozen.expectedQuantity)}</dd></div>
                  <div>
                    <dt>Quantidade contada</dt>
                    <dd>{quantity(Number(scaledQuantity(frozen.expectedQuantity) + scaledQuantity(frozen.quantity)) / 10)}</dd>
                  </div>
                </>
              )}
              <div><dt>Observação</dt><dd>{frozen.notes || "—"}</dd></div>
            </dl>
          </>
        ) : (
          <fieldset disabled={busy || Boolean(storageError) || !has("stock.adjust")} className="customer-form-fields">
            <div className="form-grid">
              <label>
                Operação
                <FilterSelect value={value.type} onChange={(event) => setValue((old) => ({ ...old, type: event.target.value }))}>
                  <option value="INBOUND">Entrada manual</option>
                  <option value="OUTBOUND">Saída manual</option>
                  <option value="CORRECTION">Corrigir saldo</option>
                </FilterSelect>
              </label>
              <label>
                {value.type === "CORRECTION" ? "Quantidade contada" : "Quantidade"}
                {` (${stock.product.unit || "unidade pendente"})`}
                <input required inputMode="decimal" value={value.quantity}
                  onChange={(event) => setValue((old) => ({ ...old, quantity: event.target.value }))} />
              </label>
              <p className="field-hint span-all">
                {value.type === "CORRECTION"
                  ? "Informe o total físico contado no depósito. O sistema calcula a diferença para corrigir o saldo."
                  : value.type === "OUTBOUND"
                    ? "Informe a quantidade a retirar, sem sinal negativo."
                    : "Informe a quantidade a adicionar ao estoque."}
              </p>
              <div className="stock-movement-preview span-all" aria-live="polite">
                <dl>
                  <div><dt>Estoque atual</dt><dd>{quantity(currentStock.quantity)}</dd></div>
                  <div><dt>Reservado</dt><dd>{quantity(currentStock.reservedQuantity)}</dd></div>
                  <div><dt>Saldo após a operação</dt><dd>{preview ? quantity(preview.next) : "—"}</dd></div>
                </dl>
                {previewError && <p>{previewError}</p>}
                {preview && value.type === "CORRECTION" && (
                  <p>
                    {preview.difference === 0
                      ? "O saldo já corresponde à contagem informada."
                      : `Correção de ${preview.difference > 0 ? "+" : ""}${quantity(preview.difference)} ${stock.product.unit}.`}
                  </p>
                )}
              </div>
              <label className="span-all">
                Observação (opcional)
                <textarea rows={3} value={value.notes}
                  onChange={(event) => setValue((old) => ({ ...old, notes: event.target.value }))} />
              </label>
            </div>
          </fieldset>
        )}
        <div className="actions">
          <button type="button" className="secondary" onClick={close} disabled={busy}>
            {frozen ? "Fechar e retomar depois" : "Cancelar"}
          </button>
          {frozen && (
            <button type="button" className="secondary" onClick={verifyMovement}
              disabled={busy || Boolean(storageError)}>
              {busy ? "Consultando…" : "Verificar no histórico"}
            </button>
          )}
          {frozen && rejectedRequestId === frozen.requestId && (
            <button type="button" className="secondary" onClick={discardRejected}
              disabled={busy || Boolean(storageError)}>
              Descartar tentativa rejeitada
            </button>
          )}
          <button
            disabled={busy || Boolean(storageError) || !has("stock.adjust") ||
              (!frozen && (!preview || Boolean(previewError) ||
                (value.type === "CORRECTION" && preview.difference === 0)))}
          >
            {busy ? "Registrando…" : frozen ? "Repetir a mesma tentativa" : "Registrar movimento"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
