import usePostalCode from "../Customers/usePostalCode";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import FilterSelect from "../../components/feedback/FilterSelect";

export const addressTypes = { MAIN: "Principal", BILLING: "Cobrança", SHIPPING: "Entrega" };
export const emptySupplierAddress = {
  country: "Brasil",
  addressType: "MAIN",
  zipCode: "",
  state: "",
  city: "",
  neighborhood: "",
  street: "",
  number: "",
  complement: "",
};
export const supplierAddressKeys = Object.keys(emptySupplierAddress);

export default function SupplierAddressFields({ value, onChange, disabled = false }) {
  const brazilian = ["brasil", "br", "bra"].includes(value.country.trim().toLowerCase());
  return (
    <div className="span-all">
      <label>
        País
        <input
          required
          value={value.country}
          disabled={disabled}
          onChange={(event) => onChange({ ...value, country: event.target.value })}
        />
      </label>
      {brazilian ? (
        <BrazilianFields
          key={value.country}
          value={value}
          onChange={onChange}
          disabled={disabled}
        />
      ) : (
        <AddressFields value={value} disabled={disabled} change={onChange} />
      )}
    </div>
  );
}

function BrazilianFields({ value, onChange, disabled }) {
  const postal = usePostalCode(value, onChange, disabled);
  return <AddressFields value={value} disabled={disabled} change={postal.change} postal={postal} />;
}

function AddressFields({ value, disabled, change, postal }) {
  const types = Object.keys(addressTypes);
  return (
    <fieldset className="customer-address-fields span-all" disabled={disabled}>
      <legend>Endereço</legend>
      <div className="form-grid">
        <label>
          Tipo
          <FilterSelect
            disabled={disabled}
            value={value.addressType}
            onChange={(event) => change({ ...value, addressType: event.target.value })}
          >
            {types.map((type) => (
              <option key={type} value={type}>
                {addressTypes[type]}
              </option>
            ))}
          </FilterSelect>
        </label>
        <label>
          CEP
          <div className="postal-control">
            <input
              required
              autoComplete="postal-code"
              value={value.zipCode}
              onChange={(event) => change({ ...value, zipCode: event.target.value })}
            />
            {postal && (
              <button
                type="button"
                className="secondary"
                disabled={postal.loading}
                onClick={postal.lookup}
              >
                {postal.loading ? "Consultando…" : "Consultar CEP"}
              </button>
            )}
          </div>
        </label>
        <div className="span-all">
          <ErrorAlert message={postal?.error} />
        </div>
        {[
          ["state", "UF"],
          ["city", "Cidade"],
          ["neighborhood", "Bairro"],
          ["street", "Logradouro"],
          ["number", "Número"],
          ["complement", "Complemento (opcional)"],
        ].map(([field, label]) => (
          <label key={field}>
            {label}
            <input
              required={field !== "complement"}
              value={value[field]}
              onChange={(event) => change({ ...value, [field]: event.target.value })}
            />
          </label>
        ))}
      </div>
    </fieldset>
  );
}
