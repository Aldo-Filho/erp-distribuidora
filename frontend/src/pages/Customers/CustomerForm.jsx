import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { removeDraft, useDraft } from "../../auth/drafts";
import { customers } from "../../services/customers";
import { prices } from "../../services/prices";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import CustomerAddressFields from "./CustomerAddressFields";
import { customerPayload, customerValues, validateCustomer } from "./customerFormData";

function priceLabel(table) {
  const date = (value) => value?.split("-").reverse().join("/");
  return (
    table.name +
    (!table.active ? " (inativa)" : "") +
    (table.startDate || table.endDate
      ? ` · ${date(table.startDate) || "Início livre"} a ${date(table.endDate) || "sem término"}`
      : "")
  );
}

export default function CustomerForm({ customer, onSaved, onClose }) {
  const { session, has } = useAuth();
  const key = `customer:${session.userId}:${customer?.id || "new"}`;
  const initial = useRef(customerValues(customer)).current;
  const [value, setValue] = useDraft(key, initial);
  const [saving, setSaving] = useState(false);
  const locked = useRef(false);
  const live = useRef(true);
  const [error, setError] = useState("");
  const [priceState, setPriceState] = useState({ loading: true });
  const [priceRevision, setPriceRevision] = useState(0);
  const canReadPrice = has("prices.read");
  const canWrite = has(customer ? "customers.update" : "customers.create");
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    if (!canReadPrice) {
      setPriceState({});
      return;
    }
    setPriceState({ loading: true });
    prices.search({ active: true }, { signal: controller.signal }).then(
      (tables) => {
        if (active) setPriceState({ tables });
      },
      (issue) => {
        if (active) setPriceState({ error: issue.message });
      },
    );
    return () => {
      active = false;
      controller.abort();
    };
  }, [canReadPrice, priceRevision]);
  const priceEnabled =
    canReadPrice && !!priceState.tables && !priceState.loading && !priceState.error;
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
  const options = [...(priceState.tables || [])];
  if (customer?.priceTable && !options.some((table) => table.id === customer.priceTable.id))
    options.unshift(customer.priceTable);
  const selectedMissing =
    value.priceTableId && !options.some((table) => String(table.id) === value.priceTableId);
  const submit = async (event) => {
    event.preventDefault();
    if (locked.current || !canWrite) return;
    const failure = validateCustomer(value, !!customer);
    if (failure) {
      setError(failure);
      return;
    }
    if (priceEnabled && selectedMissing) {
      setError(
        "A tabela selecionada não está disponível. Escolha uma tabela ativa ou Sem tabela de preços.",
      );
      return;
    }
    const payload = customerPayload(value, customer, priceEnabled);
    if (customer && !Object.keys(payload).length) {
      removeDraft(key);
      onClose();
      return;
    }
    locked.current = true;
    setSaving(true);
    setError("");
    try {
      const saved = customer
        ? await customers.update(customer.id, payload)
        : await customers.register(payload);
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
    <Modal title={customer ? "Editar cliente" : "Novo cliente"} onClose={close} busy={saving} wide>
      <ErrorAlert message={error} />
      <form className="customer-form" onSubmit={submit}>
        <fieldset disabled={saving || !canWrite} className="customer-form-fields">
          <div className="form-grid">
            <label>
              Tipo de pessoa
              <select
                disabled={!!customer}
                value={value.personType}
                onChange={(event) => change("personType", event.target.value)}
              >
                <option value="PF">Pessoa física</option>
                <option value="PJ">Pessoa jurídica</option>
              </select>
            </label>
            <label>
              {value.personType === "PF" ? "CPF" : "CNPJ"}
              <input
                required
                value={value.taxId}
                onChange={(event) => change("taxId", event.target.value)}
              />
            </label>
            <label className="span-all">
              {value.personType === "PF" ? "Nome" : "Razão social"}
              <input
                required
                value={value.legalName}
                onChange={(event) => change("legalName", event.target.value)}
              />
            </label>
            {value.personType === "PF" ? (
              <label>
                Nascimento (opcional)
                <input
                  type="date"
                  value={value.birthDate}
                  onChange={(event) => change("birthDate", event.target.value)}
                />
              </label>
            ) : (
              <>
                <label>
                  Nome fantasia
                  <input
                    required
                    value={value.tradeName}
                    onChange={(event) => change("tradeName", event.target.value)}
                  />
                </label>
                <label>
                  Inscrição estadual
                  <input
                    value={value.stateReg}
                    onChange={(event) => change("stateReg", event.target.value)}
                  />
                </label>
                <label>
                  Inscrição municipal
                  <input
                    value={value.municipalReg}
                    onChange={(event) => change("municipalReg", event.target.value)}
                  />
                </label>
              </>
            )}
            <label>
              Telefone
              <input
                type="tel"
                required
                value={value.phone}
                onChange={(event) => change("phone", event.target.value)}
              />
            </label>
            <label>
              E-mail (opcional)
              <input
                type="email"
                value={value.email}
                onChange={(event) => change("email", event.target.value)}
              />
            </label>
            <label>
              WhatsApp (opcional)
              <input
                type="tel"
                value={value.whatsapp}
                onChange={(event) => change("whatsapp", event.target.value)}
              />
            </label>
            <label className="span-all">
              Tabela de preços
              <select
                disabled={!priceEnabled}
                value={value.priceTableId}
                onChange={(event) => change("priceTableId", event.target.value)}
              >
                <option value="">Sem tabela de preços</option>
                {selectedMissing && (
                  <option disabled value={value.priceTableId}>
                    Tabela selecionada indisponível
                  </option>
                )}
                {options.map((table) => (
                  <option key={table.id} value={table.id}>
                    {priceLabel(table)}
                  </option>
                ))}
              </select>
              {!canReadPrice && (
                <small>
                  {customer?.priceTable
                    ? "Associação atual preservada. Consulta de preços sem permissão."
                    : "Consulta de preços sem permissão. Cadastro sem tabela disponível."}
                </small>
              )}
              {priceState.loading && canReadPrice && (
                <small role="status">Carregando tabelas…</small>
              )}
            </label>
            <div className="span-all">
              <ErrorAlert
                message={
                  priceState.error
                    ? "Não foi possível carregar as tabelas. A associação atual será preservada."
                    : ""
                }
                retry={() => setPriceRevision((number) => number + 1)}
              />
            </div>
            {!customer && (
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
                  <CustomerAddressFields
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
          <button disabled={saving || !canWrite}>{saving ? "Salvando…" : "Salvar cliente"}</button>
        </div>
      </form>
    </Modal>
  );
}
