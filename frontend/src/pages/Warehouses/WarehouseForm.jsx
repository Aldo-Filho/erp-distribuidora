import { WarehouseAddressFields } from "./WarehouseAddressFields.jsx";

export function WarehouseForm({
  warehouse,
  form,
  onChange,
  onSubmit,
  onCancel,
  saving,
  error,
  addressForm,
  onAddressChange,
  createdWarehouse,
}) {
  return (
    <form className="grid gap-4" onSubmit={onSubmit}>
      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
      <label className="grid gap-1.5 text-xs font-semibold text-slate-700">
        Nome
        <input
          className="h-10 rounded-lg border border-slate-300 px-3 text-sm outline-blue-600 disabled:bg-slate-100"
          name="name"
          value={form.name}
          onChange={onChange}
          required
          disabled={saving || warehouse !== "new" || Boolean(createdWarehouse)}
        />
      </label>
      {warehouse !== "new" && (
        <p className="text-xs text-slate-500">
          O nome é definido no cadastro. A edição permite alterar a descrição.
        </p>
      )}
      <label className="grid gap-1.5 text-xs font-semibold text-slate-700">
        Descrição
        <textarea
          className="min-h-24 rounded-lg border border-slate-300 p-3 text-sm outline-blue-600"
          name="description"
          value={form.description}
          onChange={onChange}
          required={warehouse !== "new"}
          disabled={saving || Boolean(createdWarehouse)}
        />
      </label>
      {warehouse === "new" && (
        <section
          className="grid gap-4 border-t border-slate-200 pt-4"
          aria-label="Endereço do novo armazém"
        >
          <h3 className="text-sm font-semibold text-slate-800">Endereço do armazém</h3>
          <WarehouseAddressFields form={addressForm} onChange={onAddressChange} disabled={saving} />
        </section>
      )}
      <div className="flex justify-end gap-2 border-t border-slate-200 pt-4">
        <button
          type="button"
          className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          onClick={onCancel}
          disabled={saving}
        >
          Cancelar
        </button>
        <button
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          disabled={saving}
        >
          {saving ? "Salvando..." : createdWarehouse ? "Salvar endereço" : "Salvar armazém"}
        </button>
      </div>
    </form>
  );
}
