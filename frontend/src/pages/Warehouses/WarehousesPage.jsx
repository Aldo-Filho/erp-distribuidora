import { useCallback, useEffect, useMemo, useState } from "react";
import { Plus, Search, Pencil, Trash2, MapPin } from "lucide-react";
import { Modal } from "../../components/Modal";
import { api } from "../../services/api";
import { WarehouseForm } from "./WarehouseForm";
import { WarehouseAddressModal } from "./WarehouseAddressModal";
import {
  emptyWarehouseAddress,
  warehouseAddressFields,
} from "./warehouseAddressConfig.js";

export function WarehousesPage() {
  const [warehouses, setWarehouses] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [selected, setSelected] = useState(null);
  const [addressWarehouse, setAddressWarehouse] = useState(null);
  const [form, setForm] = useState({ name: "", description: "" });
  const [addressForm, setAddressForm] = useState({ ...emptyWarehouseAddress });
  const [createdWarehouse, setCreatedWarehouse] = useState(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setWarehouses(await api("/warehouse/search"));
      setError("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- Carregamento inicial da API; os dados chegam de forma assíncrona.
    loadData();
  }, [loadData]);

  const filtered = useMemo(
    () =>
      warehouses.filter((warehouse) =>
        warehouse.name
          .toLocaleLowerCase("pt-BR")
          .includes(search.trim().toLocaleLowerCase("pt-BR")),
      ),
    [warehouses, search],
  );

  function openForm(warehouse = "new") {
    setSelected(warehouse);
    setFormError("");
    setCreatedWarehouse(null);
    setAddressForm({ ...emptyWarehouseAddress });
    setForm(
      warehouse === "new"
        ? { name: "", description: "" }
        : { name: warehouse.name, description: warehouse.description || "" },
    );
  }

  async function save(event) {
    event.preventDefault();
    if (saving) return;
    const name = form.name.trim();
    const description = form.description.trim();
    const addressValues = Object.fromEntries(
      warehouseAddressFields.map(({ name }) => [
        name,
        addressForm[name].trim(),
      ]),
    );
    if (!name || (selected !== "new" && !description)) {
      setFormError("Preencha os campos obrigatórios.");
      return;
    }
    if (
      selected === "new" &&
      Object.values(addressValues).some((value) => !value)
    ) {
      setFormError("Preencha todos os campos do endereço.");
      return;
    }
    setSaving(true);
    setFormError("");
    let registeredWarehouse = createdWarehouse;
    try {
      if (selected === "new") {
        // Mantém o armazém criado para repetir somente o endereço em caso de falha.
        if (!registeredWarehouse) {
          registeredWarehouse = await api("/warehouse", {
            method: "POST",
            body: JSON.stringify({ name, description }),
          });
          setCreatedWarehouse(registeredWarehouse);
        }
        await api("/warehouseAddress", {
          method: "POST",
          body: JSON.stringify({
            warehouseId: registeredWarehouse.id,
            ...addressValues,
          }),
        });
      } else {
        await api(`/warehouse/${selected.id}`, {
          method: "PATCH",
          body: JSON.stringify({ description }),
        });
      }
      setSelected(null);
      await loadData();
    } catch (requestError) {
      setFormError(
        registeredWarehouse
          ? `O armazém foi criado, mas o endereço não pôde ser salvo. Corrija os dados e clique em Salvar endereço para tentar novamente. ${requestError.message}`
          : requestError.message,
      );
      if (registeredWarehouse) await loadData();
    } finally {
      setSaving(false);
    }
  }

  async function remove(warehouse) {
    if (deletingId !== null || !window.confirm(`Excluir “${warehouse.name}”?`))
      return;
    setDeletingId(warehouse.id);
    setError("");
    try {
      await api(`/warehouse/${warehouse.id}`, { method: "DELETE" });
      await loadData();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <>
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Armazéns
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {warehouses.length}{" "}
            {warehouses.length === 1
              ? "armazém cadastrado"
              : "armazéns cadastrados"}
          </p>
        </div>
        <button
          className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
          onClick={() => openForm()}
        >
          <Plus size={17} aria-hidden="true" />
          Novo armazém
        </button>
      </header>
      <label className="mb-5 flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-slate-400 shadow-sm sm:max-w-md">
        <Search size={18} aria-hidden="true" />
        <input
          className="w-full bg-transparent text-sm text-slate-800 outline-none"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar por nome..."
          aria-label="Buscar armazém por nome"
        />
      </label>
      {error && (
        <div
          className="mb-4 flex items-center justify-between gap-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
          role="alert"
        >
          {error}
          <button
            onClick={loadData}
            className="shrink-0 font-semibold underline"
          >
            Tentar novamente
          </button>
        </div>
      )}
      <section className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full min-w-[560px] text-left text-sm text-slate-800">
          <thead className="border-b border-slate-200 text-xs text-slate-500">
            <tr>
              <th className="px-4 py-3 font-medium">Nome</th>
              <th className="px-4 py-3 font-medium">Descrição</th>
              <th className="px-4 py-3 font-medium">Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading || filtered.length === 0 ? (
              <tr>
                <td colSpan={3} className="p-10 text-center text-slate-500">
                  {loading
                    ? "Carregando armazéns..."
                    : "Nenhum armazém encontrado."}
                </td>
              </tr>
            ) : (
              filtered.map((warehouse) => (
                <tr
                  key={warehouse.id}
                  className="border-b border-slate-200 last:border-0"
                >
                  <td className="px-4 py-4 font-semibold">{warehouse.name}</td>
                  <td className="px-4 py-4">{warehouse.description || "—"}</td>
                  <td className="px-4 py-4">
                    <div className="flex gap-2">
                      <button
                        className="inline-flex items-center gap-1.5 rounded-md px-2 py-2 text-blue-600 hover:bg-blue-50"
                        onClick={() => setAddressWarehouse(warehouse)}
                        disabled={deletingId !== null}
                        aria-label={`Visualizar endereço de ${warehouse.name}`}
                      >
                        <MapPin size={17} aria-hidden="true" />
                        Ver endereço
                      </button>
                      <button
                        className="rounded-md p-2 text-slate-500 hover:bg-slate-100"
                        onClick={() => openForm(warehouse)}
                        aria-label={`Editar ${warehouse.name}`}
                      >
                        <Pencil size={17} aria-hidden="true" />
                      </button>
                      <button
                        className="rounded-md p-2 text-red-600 hover:bg-red-50 disabled:opacity-50"
                        disabled={deletingId !== null}
                        onClick={() => remove(warehouse)}
                        aria-label={`Excluir ${warehouse.name}`}
                      >
                        <Trash2 size={17} aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
      {addressWarehouse && (
        <WarehouseAddressModal
          key={addressWarehouse.id}
          warehouse={addressWarehouse}
          onClose={() => setAddressWarehouse(null)}
        />
      )}
      {selected && (
        <Modal
          title={selected === "new" ? "Novo armazém" : "Editar armazém"}
          onClose={() => {
            if (!saving) setSelected(null);
          }}
        >
          <WarehouseForm
            warehouse={selected}
            form={form}
            saving={saving}
            error={formError}
            addressForm={addressForm}
            createdWarehouse={createdWarehouse}
            onAddressChange={(event) =>
              setAddressForm((current) => ({
                ...current,
                [event.target.name]: event.target.value,
              }))
            }
            onSubmit={save}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                [event.target.name]: event.target.value,
              }))
            }
            onCancel={() => setSelected(null)}
          />
        </Modal>
      )}
    </>
  );
}
