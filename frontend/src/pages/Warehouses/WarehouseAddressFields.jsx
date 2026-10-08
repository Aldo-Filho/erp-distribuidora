import { warehouseAddressFields } from "./warehouseAddressConfig.js";

export function WarehouseAddressFields({ form, onChange, disabled }) {
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2">
        {warehouseAddressFields.map(({ name, label, maxLength, placeholder }) => (
          <label
            key={name}
            className={`grid gap-1.5 text-xs font-semibold text-slate-700 ${name === "complement" ? "sm:col-span-2" : ""}`}
          >
            {label}
            <input
              className="h-10 min-w-0 rounded-lg border border-slate-300 px-3 text-sm outline-blue-600 disabled:bg-slate-100"
              name={name}
              value={form[name]}
              maxLength={maxLength}
              placeholder={placeholder}
              required
              disabled={disabled}
              onChange={onChange}
            />
          </label>
        ))}
      </div>
      <p className="text-xs text-slate-500">
        Todos os campos do endereço são obrigatórios. Se não houver complemento, informe “Sem
        complemento”.
      </p>
    </>
  );
}
