import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import { Modal } from "../../components/Modal";
import { api } from "../../services/api";

export function ClassificationModal({
  type,
  items,
  loading,
  onCreated,
  onDeleted,
  onClose,
}) {
  const [name, setName] = useState("");
  const [pending, setPending] = useState(null);
  const [error, setError] = useState("");
  const isBrand = type === "brand";
  const singular = isBrand ? "marca" : "categoria";
  const plural = isBrand ? "Marcas" : "Categorias";
  const busy = pending !== null;
  const sortedItems = [...items].sort((first, second) =>
    first.name.localeCompare(second.name, "pt-BR"),
  );

  async function createItem(event) {
    event.preventDefault();
    if (busy || loading || !name.trim()) return;
    setPending("create");
    setError("");
    try {
      const item = await api(`/${type}`, {
        method: "POST",
        body: JSON.stringify({ name: name.trim() }),
      });
      onCreated(item);
      setName("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setPending(null);
    }
  }

  async function deleteItem(item) {
    if (busy) return;
    setPending(item.id);
    setError("");
    try {
      await api(`/${type}/${item.id}`, { method: "DELETE" });
      onDeleted(item.id);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setPending(null);
    }
  }

  return (
    <Modal
      title={plural}
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      <form className="mb-6 grid gap-3" onSubmit={createItem}>
        <label className="grid gap-1.5 text-xs font-semibold text-slate-700">
          Nome da {singular}
          <input
            className="h-10 rounded-lg border border-slate-300 px-3 text-sm outline-blue-600 disabled:bg-slate-50"
            placeholder={isBrand ? "Ex.: Samsung" : "Ex.: Eletrônicos"}
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={100}
            disabled={busy || loading}
            required
          />
        </label>
        <button
          type="submit"
          className="inline-flex h-10 items-center justify-center gap-2 justify-self-start rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          disabled={busy || loading || !name.trim()}
        >
          <Plus size={17} strokeWidth={1.8} aria-hidden="true" />
          {pending === "create" ? "Adicionando..." : `Adicionar ${singular}`}
        </button>
      </form>

      {error && (
        <p
          role="alert"
          className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
        >
          {error}
        </p>
      )}

      <section
        className="border-t border-slate-200 pt-4"
        aria-label={`${plural} existentes`}
      >
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-800">
            {plural} existentes
          </h3>
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
            {items.length}
          </span>
        </div>
        {loading ? (
          <p className="py-6 text-center text-sm text-slate-500">
            Carregando {plural.toLowerCase()}...
          </p>
        ) : sortedItems.length === 0 ? (
          <p className="rounded-lg border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
            Nenhuma {singular} cadastrada.
          </p>
        ) : (
          <ul className="max-h-72 divide-y divide-slate-100 overflow-y-auto rounded-lg border border-slate-200">
            {sortedItems.map((item) => (
              <li
                key={item.id}
                className="flex items-center justify-between gap-3 px-3 py-2.5"
              >
                <span className="min-w-0 break-words text-sm text-slate-700">
                  {item.name}
                </span>
                <button
                  type="button"
                  className="inline-flex shrink-0 items-center gap-1.5 rounded-md px-2 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
                  onClick={() => deleteItem(item)}
                  disabled={busy}
                  aria-label={`Excluir ${singular} ${item.name}`}
                >
                  <Trash2 size={15} strokeWidth={1.8} aria-hidden="true" />
                  {pending === item.id ? "Excluindo..." : "Excluir"}
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
      <div className="mt-5 flex justify-end">
        <button
          type="button"
          onClick={onClose}
          disabled={busy}
          className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          Fechar
        </button>
      </div>
    </Modal>
  );
}
