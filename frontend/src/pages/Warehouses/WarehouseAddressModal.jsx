import { useEffect, useState } from "react";
import { Pencil, Trash2 } from "lucide-react";
import { Modal } from "../../components/Modal";
import { api } from "../../services/api";
import { WarehouseAddressFields } from "./WarehouseAddressFields.jsx";
import {
  emptyWarehouseAddress as emptyForm,
  warehouseAddressFields as fields,
} from "./warehouseAddressConfig.js";

export function WarehouseAddressModal({ warehouse, onClose }) {
  const [address, setAddress] = useState(null);
  const [form, setForm] = useState({ ...emptyForm });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [pending, setPending] = useState(null);
  const [reload, setReload] = useState(0);
  const [editing, setEditing] = useState(false);
  const busy = pending !== null;

  useEffect(() => {
    let active = true;
    async function loadAddress() {
      try {
        const addresses = await api(
          `/warehouseAddress/search?warehouseId=${encodeURIComponent(warehouse.id)}`,
        );
        if (!active) return;
        const currentAddress = addresses[0] || null;
        setAddress(currentAddress);
        setForm(Object.fromEntries(fields.map(({ name }) => [name, currentAddress?.[name] || ""])));
        setLoadError("");
      } catch (requestError) {
        if (active) setLoadError(requestError.message);
      } finally {
        if (active) setLoading(false);
      }
    }
    loadAddress();
    return () => {
      active = false;
    };
  }, [warehouse.id, reload]);

  async function save(event) {
    event.preventDefault();
    if (busy || loading || loadError || !editing) return;
    const values = Object.fromEntries(fields.map(({ name }) => [name, form[name].trim()]));
    setNotice("");
    if (Object.values(values).some((value) => !value)) {
      setError("Preencha todos os campos do endereço.");
      return;
    }
    setPending("save");
    setError("");
    try {
      const saved = await api(address ? `/warehouseAddress/${address.id}` : "/warehouseAddress", {
        method: address ? "PATCH" : "POST",
        body: JSON.stringify({ warehouseId: warehouse.id, ...values }),
      });
      setAddress(saved);
      setForm(Object.fromEntries(fields.map(({ name }) => [name, saved[name] || ""])));
      setNotice("Endereço salvo com sucesso.");
      setEditing(false);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setPending(null);
    }
  }

  async function remove() {
    if (busy || !address || !window.confirm(`Excluir o endereço de “${warehouse.name}”?`)) return;
    setPending("delete");
    setError("");
    setNotice("");
    try {
      await api(`/warehouseAddress/${address.id}`, { method: "DELETE" });
      setAddress(null);
      setForm({ ...emptyForm });
      setNotice("Endereço excluído com sucesso.");
      setEditing(false);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setPending(null);
    }
  }

  return (
    <Modal
      title={`Endereço de ${warehouse.name}`}
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      {loading ? (
        <p className="py-8 text-center text-sm text-slate-500">Carregando endereço...</p>
      ) : loadError ? (
        <div
          className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
          role="alert"
        >
          <p>{loadError}</p>
          <button
            className="mt-2 font-semibold underline"
            onClick={() => {
              setLoading(true);
              setReload((current) => current + 1);
            }}
          >
            Tentar novamente
          </button>
        </div>
      ) : !editing ? (
        <>
          {notice && (
            <p className="mb-4 rounded-lg bg-green-50 p-3 text-sm text-green-700" role="status">
              {notice}
            </p>
          )}
          {address ? (
            <dl className="grid gap-4 sm:grid-cols-2">
              {fields.map(({ name, label }) => (
                <div key={name} className={name === "complement" ? "sm:col-span-2" : ""}>
                  <dt className="text-xs font-semibold text-slate-500">{label}</dt>
                  <dd className="mt-1 break-words text-sm text-slate-800">
                    {address[name] || "—"}
                  </dd>
                </div>
              ))}
            </dl>
          ) : (
            <p className="text-sm text-slate-500">
              Este armazém ainda não possui endereço cadastrado.
            </p>
          )}
          <div className="mt-5 flex justify-end gap-2 border-t border-slate-200 pt-4">
            <button
              type="button"
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              onClick={onClose}
            >
              Fechar
            </button>
            <button
              type="button"
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
              onClick={() => {
                setEditing(true);
                setError("");
                setNotice("");
              }}
            >
              <Pencil size={16} aria-hidden="true" />
              {address ? "Editar endereço" : "Cadastrar endereço"}
            </button>
          </div>
        </>
      ) : (
        <form className="grid gap-4" onSubmit={save}>
          <p className="text-sm text-slate-500">
            {address
              ? "Edite o endereço cadastrado deste armazém."
              : "Este armazém ainda não possui endereço. Preencha os campos para cadastrá-lo."}
          </p>
          {error && (
            <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">
              {error}
            </p>
          )}
          {notice && (
            <p className="rounded-lg bg-green-50 p-3 text-sm text-green-700" role="status">
              {notice}
            </p>
          )}
          <WarehouseAddressFields
            form={form}
            disabled={busy}
            onChange={(event) => {
              setForm((current) => ({
                ...current,
                [event.target.name]: event.target.value,
              }));
              setNotice("");
            }}
          />
          <div className="flex flex-wrap justify-end gap-2 border-t border-slate-200 pt-4">
            {address && (
              <button
                type="button"
                className="mr-auto inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
                onClick={remove}
                disabled={busy}
              >
                <Trash2 size={16} aria-hidden="true" />
                {pending === "delete" ? "Excluindo..." : "Excluir endereço"}
              </button>
            )}
            <button
              type="button"
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              onClick={() => {
                setForm(
                  Object.fromEntries(fields.map(({ name }) => [name, address?.[name] || ""])),
                );
                setEditing(false);
                setError("");
                setNotice("");
              }}
              disabled={busy}
            >
              Cancelar
            </button>
            <button
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              disabled={busy}
            >
              {pending === "save"
                ? "Salvando..."
                : address
                  ? "Salvar endereço"
                  : "Cadastrar endereço"}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
}
