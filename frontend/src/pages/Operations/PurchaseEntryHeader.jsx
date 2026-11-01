import { useCallback, useEffect, useState } from "react";
import { readDraft } from "../../auth/drafts";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import EmbeddedContent from "../../components/layout/EmbeddedContent";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useProductForm from "../Products/useProductForm";
import { PartyPicker, partyName } from "./OperationSelectors";

export default function PurchaseEntryHeader(props) {
  const { purchase, disabled, onStateChange, onClose, onClosed, onSaved } = props;
  const { has, session } = useAuth();
  const [editing, setEditing] = useState(() => {
    if (!purchase) return false;
    const draft = readDraft(`products:${session.userId}:school:purchase:${purchase.id}`);
    const initial = {
      supplier: purchase.supplier,
      expectedDeliveryDate: purchase.expectedDeliveryDate || "",
    };
    return Boolean(draft && JSON.stringify(draft) !== JSON.stringify(initial));
  });
  const report = useCallback(
    (saving, dirty) => onStateChange?.(saving, dirty, true),
    [onStateChange],
  );
  useEffect(() => {
    if (purchase && !editing) onStateChange?.(false, false, false);
  }, [Boolean(purchase), editing, onStateChange]);
  if (!purchase || editing) {
    return (
      <PurchaseHeaderForm
        {...props}
        onStateChange={report}
        onClose={() => {
          if (purchase) {
            setEditing(false);
            onClosed?.();
          } else onClose();
        }}
        onSaved={(saved) => {
          setEditing(false);
          onSaved(saved);
        }}
      />
    );
  }
  return (
    <div className="order-entry-header purchase-entry-header">
      <div>
        <span className="field-hint">Fornecedor</span>
        <strong>{partyName(purchase.supplier)}</strong>
      </div>
      <div>
        <span className="field-hint">Entrega prevista</span>
        <strong>
          {purchase.expectedDeliveryDate
            ? purchase.expectedDeliveryDate.split("-").reverse().join("/")
            : "Não informada"}
        </strong>
      </div>
      {has("purchases.update") && (
        <button
          type="button"
          className="secondary"
          disabled={disabled}
          onClick={() => setEditing(true)}
        >
          Alterar fornecedor e previsão
        </button>
      )}
    </div>
  );
}

function PurchaseHeaderForm({
  purchase,
  onClose,
  onSaved,
  embedded = false,
  writeHeader,
  onStateChange,
  closeRef,
  disabled = false,
}) {
  const Container = embedded ? EmbeddedContent : Modal;
  const { has, session } = useAuth();
  const canWrite = has(purchase ? "purchases.update" : "purchases.create");
  const [picker, setPicker] = useState(false);
  const form = useProductForm(
    `school:purchase:${purchase?.id || "new"}`,
    {
      supplier: purchase?.supplier || null,
      expectedDeliveryDate: purchase?.expectedDeliveryDate || "",
    },
    onClose,
    onSaved,
  );
  useEffect(() => {
    onStateChange?.(form.saving, form.dirty);
    return () => onStateChange?.(false, false);
  }, [form.saving, form.dirty, onStateChange]);
  useEffect(() => {
    if (closeRef) closeRef.current = form.close;
    return () => {
      if (closeRef) closeRef.current = null;
    };
  });
  return (
    <Container
      hideTitle={embedded}
      title={purchase ? "Editar compra em rascunho" : "Nova compra"}
      onClose={form.close}
      busy={form.saving}
      wide
      className="purchase-dialog"
    >
      <form
        className="customer-form"
        onSubmit={(e) =>
          form.submit(e, (value) => {
            if (!canWrite) throw new Error("Sem permissão para salvar compras.");
            if (purchase && purchase.status !== "DRAFT") {
              throw new Error("Somente compras em rascunho podem ser editadas.");
            }
            if (purchase && !form.dirty) return;
            if (!value.supplier) throw new Error("Selecione um fornecedor.");
            const body = { supplierId: value.supplier.id };
            if (value.expectedDeliveryDate) body.expectedDeliveryDate = value.expectedDeliveryDate;
            else if (purchase?.expectedDeliveryDate) body.removeExpectedDeliveryDate = true;
            return writeHeader(purchase, purchase ? body : { ...body, userId: session.userId });
          })
        }
      >
        <ErrorAlert message={form.error} />
        <fieldset disabled={disabled || form.saving || !canWrite} className="customer-form-fields">
          <div className="form-grid">
            <label className="span-all">
              Fornecedor
              <div className="purchase-supplier-control">
                <input
                  readOnly
                  value={form.value.supplier ? partyName(form.value.supplier) : ""}
                  placeholder="Selecione um fornecedor"
                />
                <button
                  type="button"
                  className="secondary"
                  disabled={!has("suppliers.read")}
                  onClick={() => setPicker(true)}
                >
                  {form.value.supplier ? "Trocar fornecedor" : "Selecionar fornecedor"}
                </button>
              </div>
            </label>
            <label>
              Entrega prevista (opcional)
              <input
                type="date"
                value={form.value.expectedDeliveryDate}
                onChange={(e) => form.change("expectedDeliveryDate", e.target.value)}
              />
            </label>
            {!purchase && (
              <p className="field-hint span-all">
                Ao continuar, a compra será salva como rascunho. Você poderá adicionar os produtos
                em seguida.
              </p>
            )}
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            Cancelar
          </button>
          <button disabled={disabled || form.saving || !canWrite || !form.value.supplier}>
            {form.saving ? "Salvando…" : purchase ? "Salvar cabeçalho" : "Iniciar compra"}
          </button>
        </div>
      </form>
      {picker && (
        <PartyPicker
          purchase
          onClose={() => setPicker(false)}
          onSelect={(supplier) => {
            form.change("supplier", supplier);
            setPicker(false);
          }}
        />
      )}
    </Container>
  );
}
