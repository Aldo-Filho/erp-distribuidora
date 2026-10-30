import { useEffect, useState } from "react";
import { MapPin, Pencil, Plus } from "lucide-react";
import useAuth from "../../auth/useAuth";
import PageHeading from "../../components/layout/PageHeading";
import Pagination from "../../components/feedback/Pagination";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RowActions from "../../components/feedback/RowActions";
import { warehouses } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductForm from "../Products/useProductForm";
import "./warehouses.css";

export default function WarehousesPage() {
  const { has } = useAuth();
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("");
  const [page, setPage] = useState(0);
  const [flow, setFlow] = useState(null);
  const [notice, setNotice] = useState("");
  const state = useProductResource(
    async (options) => ({
      filter,
      page,
      records: await warehouses.search({ name: filter, page, size: 20 }, options),
    }),
    [filter, page],
  );
  useEffect(() => {
    const timer = setTimeout(() => { setPage(0); setFilter(query.trim()); }, 400);
    return () => clearTimeout(timer);
  }, [query]);
  const loading = Boolean(
    query.trim() !== filter || state.loading ||
      (state.data && (state.data.filter !== filter || state.data.page !== page)),
  );
  useEffect(() => {
    if (loading || state.error || !state.data) return;
    const lastPage = Math.max(0, state.data.records.totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }, [loading, state.error, state.data, page]);
  const records = loading || state.error ? null : state.data?.records?.content;
  const filtered = Boolean(filter);
  return (
    <div className="customers-page warehouses-page">
      <header className="page-title">
        <PageHeading
          title="Depósitos"
          total={state.data?.records?.totalElements}
          loading={loading}
          singular={filtered ? "depósito encontrado" : "depósito cadastrado"}
          plural={filtered ? "depósitos encontrados" : "depósitos cadastrados"}
        />
        {has("warehouses.create") && (
          <button className="customer-icon-label" onClick={() => setFlow({ type: "new" })}>
            <Plus size={17} /> Novo depósito
          </button>
        )}
      </header>
      <section className="panel customer-filters" aria-label="Filtros de depósitos">
        <label className="customer-query">
          Busca
          <input
            type="search"
            placeholder="Nome do depósito"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setQuery("");
            setFilter("");
          }}
        >
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={!loading && state.error} retry={state.reload} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <section className="panel table-panel" aria-label="Lista de depósitos" aria-busy={loading}>
        {loading ? (
          <p role="status">Carregando depósitos…</p>
        ) : state.error ? (
          <p>Não foi possível carregar os depósitos.</p>
        ) : !records?.length ? (
          <p className="empty">
            {filtered
              ? "Nenhum depósito encontrado para estes filtros."
              : "Nenhum depósito cadastrado."}
          </p>
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Depósito</th>
                    <th>Descrição</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((warehouse) => (
                    <tr key={warehouse.id}>
                      <td>
                        <strong>{warehouse.name}</strong>
                        <small>Depósito #{warehouse.id}</small>
                      </td>
                      <td>
                        <span
                          className="warehouse-description"
                          title={warehouse.description || undefined}
                        >
                          {warehouse.description || "Não informada"}
                        </span>
                      </td>
                      <td>
                        <RowActions
                          className="customer-row-actions"
                          label={`Ações de ${warehouse.name}`}
                        >
                          <button
                            type="button"
                            className="secondary icon-button"
                            aria-label={`Endereço de ${warehouse.name}`}
                            title="Endereço"
                            onClick={() => setFlow({ type: "addresses", warehouse })}
                          >
                            <MapPin size={17} />
                          </button>
                          {has("warehouses.update") && (
                            <button
                              type="button"
                              className="secondary icon-button"
                              aria-label={`Editar depósito ${warehouse.name}`}
                              title="Editar depósito"
                              onClick={() => setFlow({ type: "edit", warehouse })}
                            >
                              <Pencil size={17} />
                            </button>
                          )}
                        </RowActions>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="customer-list-footer">
              <span>
                {state.data.records.totalElements.toLocaleString("pt-BR")}{" "}
                {state.data.records.totalElements === 1 ? "registro" : "registros"}
              </span>
            </div>
          </>
        )}
      </section>
      <Pagination
        page={page}
        totalPages={state.data?.records?.totalPages || 0}
        change={setPage}
      />
      {flow?.type === "new" && (
        <WarehouseForm
          onClose={() => setFlow(null)}
          onSaved={() => {
            setFlow(null);
            setNotice("Depósito cadastrado com sucesso.");
            state.reload();
          }}
        />
      )}
      {flow?.type === "edit" && (
        <WarehouseForm
          warehouse={flow.warehouse}
          onClose={() => setFlow(null)}
          onSaved={() => {
            setFlow(null);
            setNotice("Depósito atualizado com sucesso.");
            state.reload();
          }}
        />
      )}
      {flow?.type === "addresses" && (
        <WarehouseAddresses warehouse={flow.warehouse} onClose={() => setFlow(null)} />
      )}
    </div>
  );
}
function WarehouseForm({ warehouse, onClose, onSaved }) {
  const { has } = useAuth();
  const form = useProductForm(
    `school:warehouse:${warehouse?.id || "new"}`,
    { name: warehouse?.name || "", description: warehouse?.description || "" },
    onClose,
    onSaved,
  );
  return (
    <Modal title={warehouse ? "Editar depósito" : "Novo depósito"} onClose={form.close} busy={form.saving} wide>
      <form
        className="customer-form"
        onSubmit={(e) =>
          form.submit(e, (value) => {
            if (!value.name.trim()) throw new Error("Informe o nome do depósito.");
            const body = {
              name: value.name.trim(),
              description: value.description.trim(),
            };
            if (warehouse) {
              const changes = {};
              if (body.name !== warehouse.name) changes.name = body.name;
              if (body.description !== (warehouse.description || "")) {
                changes.description = body.description;
              }
              if (!Object.keys(changes).length) return undefined;
              return warehouses.update(warehouse.id, changes);
            }
            return warehouses.register({ ...body, description: body.description || null });
          })
        }
      >
        <ErrorAlert message={form.error} />
        <fieldset
          disabled={form.saving || !has(warehouse ? "warehouses.update" : "warehouses.create")}
          className="customer-form-fields"
        >
          <div className="form-grid">
            <label className="span-all">
              Nome do depósito
              <input
                required
                value={form.value.name}
                onChange={(e) => form.change("name", e.target.value)}
              />
            </label>
            <label className="span-all">
              Descrição (opcional)
              <textarea
                rows={3}
                value={form.value.description}
                onChange={(e) => form.change("description", e.target.value)}
              />
            </label>
          </div>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
            Cancelar
          </button>
          <button disabled={form.saving || !has(warehouse ? "warehouses.update" : "warehouses.create") || (warehouse && !form.dirty)}>
            {form.saving ? "Salvando…" : warehouse ? "Salvar alterações" : "Salvar depósito"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
const addressLabels = {
  zipCode: "CEP",
  state: "UF",
  city: "Cidade",
  neighborhood: "Bairro",
  street: "Rua",
  number: "Número",
  complement: "Complemento",
};
function WarehouseAddresses({ warehouse, onClose }) {
  const { has } = useAuth();
  const state = useProductResource(
    (options) => warehouses.addresses(warehouse.id, options),
    [warehouse.id],
  );
  const [editingAddress, setEditingAddress] = useState(null);
  const [adding, setAdding] = useState(false);
  const [notice, setNotice] = useState("");
  return (
    <Modal title="Endereço do depósito" onClose={onClose} wide>
      <div className="customer-detail-title">
        <h3>{warehouse.name}</h3>
      </div>
      <dl className="customer-details">
        <div>
          <dt>Código</dt>
          <dd>#{warehouse.id}</dd>
        </div>
        <div>
          <dt>Descrição</dt>
          <dd>{warehouse.description || "Não informada"}</dd>
        </div>
      </dl>
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <ProductLoad state={state} empty="Nenhum endereço informado.">
        {state.data?.map((address) => (
          <div className="customer-address-card" key={address.id}>
            <strong>Endereço cadastrado</strong>
            <p>
              {address.street}, {address.number} · {address.complement}
              <br />
              {address.neighborhood} · {address.city}/{address.state} · CEP {address.zipCode}
            </p>
            {has("warehouses.update") && (
              <button type="button" className="secondary" onClick={() => setEditingAddress(address)}>
                <Pencil size={16} /> Editar endereço
              </button>
            )}
          </div>
        ))}
      </ProductLoad>
      {has("warehouses.create") && !state.loading && !state.error && !state.data?.length && (
        <div className="row-actions">
          <button className="secondary customer-icon-label" onClick={() => setAdding(true)}>
            <Plus size={17} /> Cadastrar endereço
          </button>
        </div>
      )}
      <div className="actions">
        <button type="button" className="secondary" onClick={onClose}>
          Fechar
        </button>
      </div>
      {editingAddress && (
        <WarehouseAddressForm
          warehouse={warehouse}
          address={editingAddress}
          onClose={() => setEditingAddress(null)}
          onSaved={() => {
            setEditingAddress(null);
            setNotice("Endereço atualizado com sucesso.");
            state.reload();
          }}
        />
      )}
      {adding && (
        <WarehouseAddressForm
          warehouse={warehouse}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false);
            setNotice("Endereço cadastrado com sucesso.");
            state.reload();
          }}
        />
      )}
    </Modal>
  );
}
function WarehouseAddressForm({ warehouse, address, onSaved, onClose }) {
  const { has } = useAuth();
  const form = useProductForm(
    `school:warehouse-address:${warehouse.id}:${address?.id || "new"}`,
    Object.fromEntries(Object.keys(addressLabels).map((key) => [key, address?.[key] || ""])),
    onClose,
    onSaved,
  );
  return (
    <Modal title={address ? "Editar endereço do depósito" : "Cadastrar endereço do depósito"} busy={form.saving} onClose={form.close} wide>
      <form
        className="customer-form"
        onSubmit={(e) =>
          form.submit(e, (value) => {
            if (Object.values(value).some((text) => !text.trim()))
              throw new Error("Preencha todos os campos do endereço.");
            const fields = Object.fromEntries(
              Object.entries(value).map(([key, text]) => [key, text.trim()]),
            );
            if (address) {
              const changes = Object.fromEntries(
                Object.entries(fields).filter(([key, text]) => text !== address[key]),
              );
              if (!Object.keys(changes).length) return undefined;
              return warehouses.updateAddress(address.id, changes);
            }
            return warehouses.registerAddress({ warehouseId: warehouse.id, ...fields });
          })
        }
      >
        <ErrorAlert message={form.error} />
        <fieldset
          disabled={form.saving || !has(address ? "warehouses.update" : "warehouses.create")}
          className="customer-form-fields"
        >
          <div className="form-grid">
            {Object.entries(addressLabels).map(([key, label]) => (
              <label
                key={key}
                className={key === "street" || key === "complement" ? "span-all" : undefined}
              >
                {label}
                <input
                  required
                  value={form.value[key]}
                  onChange={(e) => form.change(key, e.target.value)}
                />
              </label>
            ))}
          </div>
          <p className="field-hint">Todos os campos são obrigatórios, inclusive complemento.</p>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
            Cancelar
          </button>
          <button disabled={form.saving || !has(address ? "warehouses.update" : "warehouses.create") || (address && !form.dirty)}>
            {form.saving ? "Salvando…" : address ? "Salvar alterações" : "Salvar endereço"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
