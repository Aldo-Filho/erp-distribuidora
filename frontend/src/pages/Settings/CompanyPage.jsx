import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { company } from "../../services/company";
import useDirtyUnload from "../../hooks/useDirtyUnload";
import useConfirm from "../../components/feedback/useConfirm";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import FilterSelect from "../../components/feedback/FilterSelect";
import { validTaxId } from "../Customers/customerTaxId";
import usePostalCode from "../Customers/usePostalCode";

const states = "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(" ");
const identityFields = [
  ["legalName", "Razão social", 150, true],
  ["tradeName", "Nome fantasia", 150, false],
  ["taxId", "CNPJ", 18, true],
  ["stateReg", "Inscrição estadual", 20, false],
  ["municipalReg", "Inscrição municipal", 20, false],
];
const addressFields = [
  ["country", "País", 50], ["city", "Cidade", 100],
  ["street", "Logradouro", 150], ["neighborhood", "Bairro", 100],
  ["number", "Número", 10], ["complement", "Complemento", 100],
];

function values(data) {
  const result = { version: data.version, taxRegime: data.taxRegime || "", address: {} };
  for (const [field] of identityFields) result[field] = data[field] || "";
  for (const [field] of [...addressFields, ["state"], ["zipCode"]])
    result.address[field] = data.address?.[field] || (field === "country" ? "Brasil" : "");
  return result;
}

export default function CompanyPage() {
  const { has } = useAuth();
  const confirm = useConfirm();
  const [value, setValue] = useState(null);
  const [initial, setInitial] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [conflict, setConflict] = useState(false);
  const [revision, setRevision] = useState(0);
  const locked = useRef(false);
  const live = useRef(false);
  const canRead = has("company.read");
  const canWrite = has("company.manage");
  const dirty = value !== null && JSON.stringify(value) !== JSON.stringify(initial);
  useDirtyUnload(saving || dirty);

  useEffect(() => {
    live.current = true;
    return () => { live.current = false; };
  }, []);

  useEffect(() => {
    if (!canRead) return;
    const controller = new AbortController();
    let active = true;
    company.find({ signal: controller.signal }).then(data => {
      if (!active) return;
      const next = values(data);
      setValue(next);
      setInitial(next);
      setConflict(false);
    }, issue => {
      if (active) setError(issue.message);
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [canRead, revision]);

  const reload = async () => {
    if (locked.current || (dirty && !(await confirm("Descartar as alterações e carregar os dados salvos da empresa?", {
      title: "Recarregar dados", confirmLabel: "Recarregar", destructive: true,
    })))) return;
    setError("");
    setNotice("");
    setLoading(true);
    setRevision(previous => previous + 1);
  };

  const save = async event => {
    event.preventDefault();
    if (locked.current || !canWrite || loading || conflict) return;
    setNotice("");
    if (!validTaxId(value.taxId, "PJ")) {
      setError("Informe um CNPJ válido.");
      return;
    }
    locked.current = true;
    setSaving(true);
    setError("");
    try {
      const next = values(await company.save(value));
      if (live.current) {
        setValue(next);
        setInitial(next);
        setNotice("Dados da empresa salvos.");
      }
    } catch (issue) {
      if (live.current) {
        setConflict(issue.status === 409);
        setError(issue.status === 409
          ? "Os dados mudaram ou conflitam com outro cadastro. Recarregue antes de salvar novamente."
          : issue.message);
      }
    } finally {
      locked.current = false;
      if (live.current) setSaving(false);
    }
  };

  if (!canRead) return <p>Sem permissão para consultar os dados da empresa.</p>;
  return (
    <div>
      <header className="page-title">
        <div>
          <h1>Dados da empresa</h1>
          <p className="field-hint">Identificação e endereço da empresa que utiliza o JamsHub.</p>
        </div>
        <button type="button" className="secondary" disabled={loading || saving} onClick={reload}>
          Recarregar dados
        </button>
      </header>
      <ErrorAlert message={error} />
      {notice && <p className="success" role="status">{notice}</p>}
      {loading ? <p role="status">Carregando dados da empresa...</p> : value && (
        <form onSubmit={save}>
          <fieldset className="company-profile-fields" aria-label="Cadastro da empresa" disabled={!canWrite || saving}>
            <section className="panel" aria-labelledby="company-identification">
              <h2 id="company-identification">Identificação e dados fiscais</h2>
              <div className="form-grid">
                {identityFields.map(([field, label, maxLength, required]) => (
                  <label key={field}>
                    {label}{required ? " *" : " (opcional)"}
                    <input required={required} maxLength={maxLength} value={value[field]}
                      onChange={event => {
                        setNotice("");
                        setValue(previous => ({ ...previous, [field]: event.target.value }));
                      }} />
                  </label>
                ))}
                <label>
                  Regime tributário *
                  <FilterSelect required value={value.taxRegime} disabled={!canWrite || saving}
                    onChange={event => {
                      setNotice("");
                      setValue(previous => ({ ...previous, taxRegime: event.target.value }));
                    }}>
                    <option value="">Selecione</option>
                    <option value="SIMPLES_NACIONAL">Simples Nacional</option>
                    <option value="LUCRO_PRESUMIDO">Lucro Presumido</option>
                    <option value="LUCRO_REAL">Lucro Real</option>
                  </FilterSelect>
                </label>
              </div>
            </section>
            <CompanyAddress value={value.address} disabled={!canWrite || saving}
              onChange={address => {
                setNotice("");
                setValue(previous => ({ ...previous, address }));
              }} />
          </fieldset>
          <p className="field-hint">
            Os novos documentos demonstrativos usarão estes dados. Documentos já emitidos mantêm os dados originais.
            A emissão atual é demonstrativa, sem validade fiscal.
          </p>
          {canWrite ? <div className="actions">
            <button disabled={saving || !dirty || conflict}>{saving ? "Salvando..." : "Salvar dados da empresa"}</button>
          </div> : <p className="field-hint">Você possui acesso somente para consulta.</p>}
        </form>
      )}
    </div>
  );
}

function CompanyAddress({ value, disabled, onChange }) {
  const postal = usePostalCode(value, onChange, disabled);
  return (
    <section className="panel" aria-labelledby="company-address">
      <h2 id="company-address">Endereço da empresa</h2>
      <div className="form-grid">
        <label>
          CEP *
          <div className="postal-control">
            <input required maxLength={9} pattern="[0-9]{5}-?[0-9]{3}" autoComplete="postal-code"
              value={value.zipCode} onChange={event => postal.change({ ...value, zipCode: event.target.value })} />
            <button type="button" className="secondary" disabled={disabled || postal.loading} onClick={postal.lookup}>
              {postal.loading ? "Consultando..." : "Consultar CEP"}
            </button>
          </div>
        </label>
        <label>
          UF *
          <FilterSelect required value={value.state} disabled={disabled}
            onChange={event => postal.change({ ...value, state: event.target.value })}>
            <option value="">Selecione</option>
            {value.state && !states.includes(value.state) && <option value={value.state}>{value.state} (revisar)</option>}
            {states.map(state => <option key={state} value={state}>{state}</option>)}
          </FilterSelect>
        </label>
        {addressFields.map(([field, label, maxLength]) => (
          <label key={field}>
            {label}{field === "complement" ? " (opcional)" : " *"}
            <input required={field !== "complement"} maxLength={maxLength} value={value[field]}
              onChange={event => postal.change({ ...value, [field]: event.target.value })} />
          </label>
        ))}
        <div className="span-all"><ErrorAlert message={postal.error} /></div>
      </div>
    </section>
  );
}
