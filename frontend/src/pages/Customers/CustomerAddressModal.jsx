import useConfirm from "../../components/feedback/useConfirm";
import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { removeDraft, useDraft } from "../../auth/drafts";
import { customers } from "../../services/customers";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import CustomerAddressFields, {
  addressKeys,
  addressTypes,
  emptyAddress,
} from "./CustomerAddressFields";
import { validateAddress } from "./customerFormData";

function AddressEditor({ customerId, address, types, onSaved, onCancel, onBusy }) {
  const confirm = useConfirm();
  const { session } = useAuth();
  const key = `customer-address:${session.userId}:${customerId}:${address?.id || "new"}`;
  const initial = useRef(
    Object.fromEntries(
      addressKeys.map((field) => [
        field,
        address?.[field] ?? (field === "addressType" ? types[0] : emptyAddress[field]),
      ]),
    ),
  ).current;
  const [value, setValue] = useDraft(key, initial);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const lock = useRef(false);
  const live = useRef(true);
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const close = async () => {
    if (lock.current) return;
    if (
      JSON.stringify(value) !== JSON.stringify(initial) &&
      !(await confirm("Descartar as alterações do endereço?", {
        title: "Descartar alterações",
        confirmLabel: "Descartar",
        destructive: true,
      }))
    )
      return;
    removeDraft(key);
    onCancel();
  };
  // Parent uses the same discard guard for Escape and its close button.
  useEffect(() => {
    onBusy(saving, close);
  });
  const submit = async (event) => {
    event.preventDefault();
    if (lock.current) return;
    const failure = validateAddress(value);
    if (failure) {
      setError(failure);
      return;
    }
    if (!types.includes(value.addressType)) {
      setError(
        "Este tipo de endereço não está disponível. Escolha outro ou recarregue os endereços.",
      );
      return;
    }
    const body = {};
    for (const field of addressKeys) {
      if (!address || value[field] !== (address[field] ?? "")) body[field] = value[field];
    }
    if (address && !Object.keys(body).length) {
      removeDraft(key);
      onCancel();
      return;
    }
    lock.current = true;
    setSaving(true);
    onBusy(true, close);
    setError("");
    try {
      if (address) await customers.updateAddress(address.id, body);
      else await customers.registerAddress({ ...body, customerId });
      removeDraft(key);
      if (live.current) onSaved();
    } catch (issue) {
      if (live.current) setError(issue.message);
    } finally {
      lock.current = false;
      if (live.current) {
        setSaving(false);
        onBusy(false, close);
      }
    }
  };
  return (
    <form onSubmit={submit} className="customer-form">
      <h3>{address ? "Editar endereço" : "Novo endereço"}</h3>
      <ErrorAlert message={error} />
      <CustomerAddressFields value={value} onChange={setValue} disabled={saving} types={types} />
      <div className="actions">
        <button type="button" className="secondary" disabled={saving} onClick={close}>
          Cancelar edição
        </button>
        <button disabled={saving}>{saving ? "Salvando…" : "Salvar endereço"}</button>
      </div>
    </form>
  );
}

export default function CustomerAddressModal({ customerId, onClose }) {
  const confirm = useConfirm();
  const { has } = useAuth();
  const [state, setState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState("");
  const [notice, setNotice] = useState("");
  const closeEditor = useRef(null);
  const lock = useRef(false);
  const live = useRef(true);
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    setState((previous) => ({ ...previous, loading: true, error: "" }));
    Promise.all([
      customers.findOne(customerId, { signal: controller.signal }),
      customers.addresses(customerId, { signal: controller.signal }),
    ]).then(
      ([customer, addresses]) => {
        if (active) setState({ customer, addresses });
      },
      (issue) => {
        if (active) setState((previous) => ({ ...previous, loading: false, error: issue.message }));
      },
    );
    return () => {
      active = false;
      controller.abort();
    };
  }, [customerId, revision]);
  const finishEditor = () => {
    closeEditor.current = null;
    setBusy(false);
    setEditing(null);
  };
  const available = Object.keys(addressTypes).filter(
    (type) =>
      !state.addresses?.some(
        (address) => address.addressType === type && address.id !== editing?.address?.id,
      ),
  );
  const remove = async (address) => {
    if (
      lock.current ||
      busy ||
      !has("customers.delete") ||
      !(await confirm(`Excluir o endereço ${addressTypes[address.addressType].toLowerCase()}?`, {
        title: "Confirmar exclusão",
        confirmLabel: "Excluir",
        destructive: true,
      }))
    )
      return;
    lock.current = true;
    setBusy(true);
    setFailure("");
    try {
      await customers.deleteAddress(address.id);
      if (live.current) {
        setNotice("Endereço excluído.");
        setRevision((number) => number + 1);
      }
    } catch (issue) {
      if (live.current) setFailure(issue.message);
    } finally {
      lock.current = false;
      if (live.current) setBusy(false);
    }
  };
  return (
    <Modal
      title="Endereços do cliente"
      wide
      busy={busy}
      onClose={() => {
        if (editing) {
          closeEditor.current?.();
          return;
        }
        onClose();
      }}
    >
      <ErrorAlert
        message={failure || state.error}
        retry={state.error ? () => setRevision((number) => number + 1) : undefined}
      />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      {state.customer && <h3>{state.customer.legalName}</h3>}
      {state.loading && <p role="status">Carregando endereços…</p>}
      {!state.loading && !state.error && !state.customer && <p>Cliente não encontrado.</p>}
      {state.customer && (
        <>
          {!editing && (
            <>
              {state.addresses?.map((address) => (
                <div className="customer-address-card" key={address.id}>
                  <strong>{addressTypes[address.addressType]}</strong>
                  <p>
                    {address.street}, {address.number}
                    {address.complement ? ` · ${address.complement}` : ""}
                    <br />
                    {address.neighborhood} · {address.city}/{address.state} · CEP {address.zipCode}
                  </p>
                  <div className="row-actions">
                    {has("customers.update") && (
                      <button
                        className="secondary"
                        disabled={busy || state.loading || !!state.error}
                        onClick={() => {
                          setNotice("");
                          setEditing({ address });
                        }}
                      >
                        Editar
                      </button>
                    )}
                    {has("customers.delete") && (
                      <button
                        className="danger-link"
                        disabled={busy || state.loading || !!state.error}
                        onClick={() => remove(address)}
                      >
                        Excluir endereço
                      </button>
                    )}
                  </div>
                </div>
              ))}
              {!state.loading && !state.error && !state.addresses?.length && (
                <p>Nenhum endereço cadastrado.</p>
              )}
              {has("customers.create") && (
                <div className="actions">
                  <button
                    disabled={busy || state.loading || !!state.error || !available.length}
                    onClick={() => {
                      setNotice("");
                      setEditing({ address: null });
                    }}
                  >
                    Novo endereço
                  </button>
                </div>
              )}
            </>
          )}
          {editing && has(editing.address ? "customers.update" : "customers.create") && (
            <>
              <AddressEditor
                key={editing.address?.id || "new"}
                customerId={customerId}
                address={editing.address}
                types={available}
                onBusy={(pending, guard) => {
                  setBusy(pending);
                  closeEditor.current = guard;
                }}
                onCancel={finishEditor}
                onSaved={() => {
                  finishEditor();
                  setNotice("Endereço salvo.");
                  setRevision((number) => number + 1);
                }}
              />
              <button
                type="button"
                className="text-button"
                disabled={busy || state.loading}
                onClick={() => setRevision((number) => number + 1)}
              >
                Recarregar tipos disponíveis
              </button>
            </>
          )}
        </>
      )}
    </Modal>
  );
}
