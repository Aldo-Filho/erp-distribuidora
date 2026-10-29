import { useEffect, useRef, useState } from "react";
import { Eye, MapPin, Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { customers } from "../../services/customers";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import Modal from "../../components/feedback/Modal";
import CustomerForm from "./CustomerForm.jsx";
import CustomerDetailsModal from "./CustomerDetailsModal";
import CustomerAddressModal from "./CustomerAddressModal";
import useCustomers, { emptyFilters } from "./useCustomers";

function EditCustomer({ customerId, onClose, onSaved }) {
  const [state, setState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setState({ loading: true });
    customers.findOne(customerId, { signal: controller.signal }).then(
      (customer) => {
        if (live) setState({ customer });
      },
      (issue) => {
        if (live) setState({ error: issue.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [customerId, revision]);
  if (state.customer)
    return (
      <CustomerForm
        key={state.customer.id}
        customer={state.customer}
        onClose={onClose}
        onSaved={onSaved}
      />
    );
  return (
    <Modal title="Editar cliente" onClose={onClose}>
      <ErrorAlert message={state.error} retry={() => setRevision((number) => number + 1)} />
      {state.loading ? (
        <p role="status">Carregando cliente…</p>
      ) : (
        !state.error && <p>Cliente não encontrado.</p>
      )}
    </Modal>
  );
}

export default function CustomersPage() {
  const { has, session } = useAuth();
  const list = useCustomers();
  const [flow, setFlow] = useState(null);
  const [failure, setFailure] = useState("");
  const [notice, setNotice] = useState("");
  const [pending, setPending] = useState(null);
  const lock = useRef(false);
  const live = useRef(true);
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const changeFilter = (field, value) => list.changeFilters({ ...list.filters, [field]: value });
  const closeFlow = () =>
    setFlow((previous) => (previous?.returnTo ? { kind: "details", id: previous.returnTo } : null));
  const saved = () => {
    setNotice("Cliente salvo.");
    list.reload();
    closeFlow();
  };
  const changeActive = async (customer) => {
    if (lock.current || !has("customers.update")) return;
    if (
      !window.confirm(`${customer.active ? "Inativar" : "Ativar"} o cliente ${customer.legalName}?`)
    )
      return;
    lock.current = true;
    setPending(customer.id);
    setFailure("");
    setNotice("");
    try {
      const updated = await customers.update(customer.id, { active: !customer.active });
      if (live.current) {
        setNotice(updated.active ? "Cliente ativado." : "Cliente inativado.");
        list.reload();
      }
    } catch (issue) {
      if (live.current) setFailure(issue.message);
    } finally {
      lock.current = false;
      if (live.current) setPending(null);
    }
  };
  const filtered = !!(
    list.filters.query.trim() ||
    list.filters.personType ||
    list.filters.active !== ""
  );
  return (
    <div className="customers-page">
      <div className="page-title">
        <h1>Clientes</h1>
        {has("customers.create") && (
          <button
            className="customer-icon-label"
            disabled={!!pending}
            onClick={() => setFlow({ kind: "new" })}
          >
            <Plus size={17} />
            Novo cliente
          </button>
        )}
      </div>
      <section className="panel customer-filters" aria-label="Filtros de clientes">
        <label>
          Pesquisar por
          <select
            value={list.filters.searchField}
            onChange={(event) => changeFilter("searchField", event.target.value)}
          >
            <option value="NAME">Nome</option>
            <option value="DOCUMENT">CPF/CNPJ</option>
            <option value="CONTACT">Contato</option>
          </select>
        </label>
        <label className="customer-query">
          Busca
          <input
            type="search"
            value={list.filters.query}
            placeholder={
              list.filters.searchField === "NAME"
                ? "Nome ou razão social"
                : list.filters.searchField === "DOCUMENT"
                  ? "CPF ou CNPJ"
                  : "E-mail ou telefone"
            }
            onChange={(event) => changeFilter("query", event.target.value)}
          />
        </label>
        <label>
          Tipo
          <select
            value={list.filters.personType}
            onChange={(event) => changeFilter("personType", event.target.value)}
          >
            <option value="">Todos</option>
            <option value="PF">Pessoa física</option>
            <option value="PJ">Pessoa jurídica</option>
          </select>
        </label>
        <label>
          Situação
          <select
            value={list.filters.active}
            onChange={(event) => changeFilter("active", event.target.value)}
          >
            <option value="">Todas</option>
            <option value="true">Ativos</option>
            <option value="false">Inativos</option>
          </select>
        </label>
        <button className="secondary" onClick={() => list.changeFilters({ ...emptyFilters })}>
          Limpar filtros
        </button>
      </section>
      <ErrorAlert message={failure || list.error} retry={list.error ? list.reload : undefined} />
      {notice && (
        <p role="status" className="success">
          {notice}
        </p>
      )}
      <section
        className="panel table-panel"
        aria-label="Lista de clientes"
        aria-busy={list.loading}
      >
        {list.loading ? (
          <p role="status">Carregando clientes…</p>
        ) : list.error ? (
          <p>Não foi possível carregar os clientes.</p>
        ) : !list.data?.content.length ? (
          <p className="empty">
            {filtered
              ? "Nenhum cliente encontrado para estes filtros."
              : "Nenhum cliente cadastrado."}
          </p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>CPF/CNPJ</th>
                  <th>Contato</th>
                  <th>Situação</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {list.data.content.map((customer) => (
                  <tr key={customer.id}>
                    <td>
                      <strong>{customer.legalName}</strong>
                      {customer.tradeName && <small>{customer.tradeName}</small>}
                    </td>
                    <td>
                      {customer.taxId}
                      <small>{customer.personType}</small>
                    </td>
                    <td>
                      {customer.phone}
                      <small>{customer.email}</small>
                      {customer.whatsapp && <small>WhatsApp: {customer.whatsapp}</small>}
                    </td>
                    <td>
                      <span className={"badge " + (customer.active ? "active" : "")}>
                        {customer.active ? "Ativo" : "Inativo"}
                      </span>
                    </td>
                    <td>
                      <div className="row-actions customer-row-actions">
                        <button
                          className="secondary icon-button"
                          aria-label={`Detalhes de ${customer.legalName}`}
                          title="Detalhes"
                          disabled={!!pending}
                          onClick={() => setFlow({ kind: "details", id: customer.id })}
                        >
                          <Eye size={17} />
                        </button>
                        {has("customers.update") && (
                          <button
                            className="secondary icon-button"
                            aria-label={`Editar ${customer.legalName}`}
                            title="Editar"
                            disabled={!!pending}
                            onClick={() => setFlow({ kind: "edit", id: customer.id })}
                          >
                            <Pencil size={17} />
                          </button>
                        )}
                        <button
                          className="secondary icon-button"
                          aria-label={`Endereços de ${customer.legalName}`}
                          title="Endereços"
                          disabled={!!pending}
                          onClick={() => setFlow({ kind: "addresses", id: customer.id })}
                        >
                          <MapPin size={17} />
                        </button>
                        {has("customers.update") && (
                          <button
                            className="secondary icon-button"
                            aria-label={`${customer.active ? "Inativar" : "Ativar"} ${customer.legalName}`}
                            title={customer.active ? "Inativar" : "Ativar"}
                            disabled={!!pending}
                            onClick={() => changeActive(customer)}
                          >
                            <Power size={17} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="customer-list-footer">
          <span>
            {list.data
              ? `${list.data.totalElements} cliente${list.data.totalElements === 1 ? "" : "s"}`
              : ""}
          </span>
          <label>
            Por página
            <select
              value={list.size}
              onChange={(event) => list.changeSize(Number(event.target.value))}
            >
              {[25, 50, 100].map((size) => (
                <option value={size} key={size}>
                  {size}
                </option>
              ))}
            </select>
          </label>
          <Pagination
            page={list.page}
            totalPages={list.data?.totalPages}
            change={list.changePage}
          />
        </div>
      </section>
      {flow?.kind === "new" && has("customers.create") && (
        <CustomerForm
          key={`new:${session.userId}`}
          customer={null}
          onClose={closeFlow}
          onSaved={saved}
        />
      )}
      {flow?.kind === "edit" && has("customers.update") && (
        <EditCustomer
          key={`${session.userId}:${flow.id}`}
          customerId={flow.id}
          onClose={closeFlow}
          onSaved={saved}
        />
      )}
      {flow?.kind === "details" && (
        <CustomerDetailsModal
          key={`details:${flow.id}`}
          customerId={flow.id}
          onClose={closeFlow}
          onEdit={(customer) => setFlow({ kind: "edit", id: customer.id, returnTo: customer.id })}
          onAddresses={(id) => setFlow({ kind: "addresses", id, returnTo: id })}
        />
      )}
      {flow?.kind === "addresses" && (
        <CustomerAddressModal
          key={`addresses:${flow.id}`}
          customerId={flow.id}
          onClose={closeFlow}
        />
      )}
    </div>
  );
}
