import usePostalCode from "./usePostalCode";
import ErrorAlert from "../../components/feedback/ErrorAlert";

export const addressTypes = { MAIN: "Principal", BILLING: "Cobrança", SHIPPING: "Entrega" };
export const emptyAddress = {
  addressType: "MAIN",
  zipCode: "",
  state: "",
  city: "",
  neighborhood: "",
  street: "",
  number: "",
  complement: "",
};
export const addressKeys = Object.keys(emptyAddress);

export default function CustomerAddressFields({
  value,
  onChange,
  disabled = false,
  types = Object.keys(addressTypes),
}) {
  const postal = usePostalCode(value, onChange, disabled);
  return (
    <fieldset className="customer-address-fields span-all" disabled={disabled}>
      <legend>Endereço</legend>
      <div className="form-grid">
        <label>
          Tipo
          <select
            autoFocus
            value={value.addressType}
            onChange={(event) => postal.change({ ...value, addressType: event.target.value })}
          >
            {types.map((type) => (
              <option key={type} value={type}>
                {addressTypes[type]}
              </option>
            ))}
          </select>
        </label>
        <label>
          CEP
          <div className="postal-control">
            <input
              required
              autoComplete="postal-code"
              value={value.zipCode}
              onChange={(event) => postal.change({ ...value, zipCode: event.target.value })}
            />
            <button
              type="button"
              className="secondary"
              disabled={postal.loading}
              onClick={postal.lookup}
            >
              {postal.loading ? "Consultando…" : "Consultar CEP"}
            </button>
          </div>
        </label>
        <div className="span-all">
          <ErrorAlert message={postal.error} />
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
              onChange={(event) => postal.change({ ...value, [field]: event.target.value })}
            />
          </label>
        ))}
      </div>
    </fieldset>
  );
}
