import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { removeDraft, useDraft } from "../../auth/drafts";
import { suppliers } from "../../services/suppliers";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import SupplierAddressFields from "./SupplierAddressFields";
import { supplierPayload, supplierValues, validateSupplier } from "./supplierFormData";

export default function SupplierForm({ supplier, onSaved, onClose }) {
  const { session, has } = useAuth();
  const key = `supplier:${session.userId}:${supplier?.id || "new"}`;
  const initial = useRef(supplierValues(supplier)).current;
  const [value, setValue] = useDraft(key, initial);
  const [saving, setSaving] = useState(false);
  const locked = useRef(false);
  const live = useRef(true);
  const [error, setError] = useState("");
  const canWrite = has(supplier ? "suppliers.update" : "suppliers.create");
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const close = () => {
    if (locked.current) return;
    if (
      JSON.stringify(value) !== JSON.stringify(initial) &&
      !window.confirm("Descartar as alterações deste formulário?")
    )
      return;
    removeDraft(key);
    onClose();
  };
  const change = (field, next) => setValue((previous) => ({ ...previous, [field]: next }));
  const submit = async (event) => {
    event.preventDefault();
    if (locked.current || !canWrite) return;
    const payload = supplierPayload(value, supplier);
    if (supplier && !Object.keys(payload).length) {
      removeDraft(key);
      onClose();
      return;
    }
    if (!event.currentTarget.reportValidity()) return;
    const failure = validateSupplier(value, !!supplier);
    if (failure) {
      setError(failure);
      return;
    }
    if ((!supplier || value.taxId !== supplier.taxId) &&
      !/^[A-Z0-9]{14}$/.test(value.taxId.replace(/[\s./-]/g, "").toUpperCase())) {
      setError("Informe um CNPJ com 14 letras ou números.");
      return;
    }
    locked.current = true;
    setSaving(true);
    setError("");
    try {
      const saved = supplier
        ? await suppliers.update(supplier.id, payload)
        : await suppliers.register(payload);
      removeDraft(key);
      if (live.current) onSaved(saved);
    } catch (issue) {
      if (live.current) setError(issue.message);
    } finally {
      locked.current = false;
      if (live.current) setSaving(false);
    }
  };
  return (
    <Modal
      title={supplier ? "Editar fornecedor" : "Novo fornecedor"}
      onClose={close}
      busy={saving}
      wide
    >
      <ErrorAlert message={error} />
      <form className="customer-form" onSubmit={submit} noValidate>
        <fieldset disabled={saving || !canWrite} className="customer-form-fields">
          <div className="form-grid">
            {[
              ["legalName", "Razão social", "text", true],
              ["taxId", "CNPJ", "text", true],
              ["tradeName", "Nome fantasia (opcional)", "text", false],
              ["phone", "Telefone", "tel", true],
              ["email", "E-mail (opcional)", "email", false],
              ["whatsapp", "WhatsApp (opcional)", "tel", false],
            ].map(([field, label, type, required]) => (
              <label key={field}>
                {label}
                <input type={type} required={required} value={value[field]}
                  onChange={(event) => change(field, event.target.value)} />
              </label>
            ))}
            <label>
              Prazo médio em dias (opcional)
              <input type="number" min="0" max="2147483647" step="1" value={value.avgDeliveryDays}
                onChange={(event) => change("avgDeliveryDays", event.target.value)} />
            </label>
            {supplier && !supplier.phone?.trim() && (
              <p className="span-all">Informe o telefone para salvar a edição deste cadastro.</p>
            )}
            {!supplier && (
              <>
                <label className="check">
                  <input
                    type="checkbox"
                    checked={value.active}
                    onChange={(event) => change("active", event.target.checked)}
                  />
                  Ativo
                </label>
                <label className="check">
                  <input
                    type="checkbox"
                    checked={value.withAddress}
                    onChange={(event) => change("withAddress", event.target.checked)}
                  />
                  Incluir endereço inicial
                </label>
                {value.withAddress && (
                  <SupplierAddressFields
                    value={value.address}
                    onChange={(address) => change("address", address)}
                    disabled={saving}
                  />
                )}
              </>
            )}
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={saving} onClick={close}>
            Cancelar
          </button>
          <button disabled={saving || !canWrite}>
            {saving ? "Salvando…" : "Salvar fornecedor"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
