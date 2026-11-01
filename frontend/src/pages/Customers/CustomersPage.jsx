import useListReturn from "../../hooks/useListReturn";
import RowActions from "../../components/feedback/RowActions";
import DocumentValue from "../../components/feedback/DocumentValue";
import useConfirm from "../../components/feedback/useConfirm";
import PageHeading from "../../components/layout/PageHeading";
import FilterSelect from "../../components/feedback/FilterSelect";
import { useEffect, useRef, useState } from "react";
import { Eye, MapPin, Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { customers } from "../../services/customers";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import useCustomers, { emptyFilters } from "./useCustomers";

export default function CustomersPage() {
  const confirm = useConfirm();
  const { has } = useAuth();
  const origin = useListReturn({ modulePath: "/customers" });
  const list = useCustomers(origin.restored);
  const listReturn = useListReturn({
    modulePath: "/customers",
    listState: { filters: list.filters, page: list.page, size: list.size },
    ready: Boolean(list.data),
  });
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
  const changeActive = async (customer) => {
    if (lock.current || !has("customers.update")) return;
    if (
      !(await confirm(
        `${customer.active ? "Inativar" : "Ativar"} o cliente ${customer.legalName}?`,
        {
          title: customer.active ? "Inativar cliente" : "Ativar cliente",
          confirmLabel: customer.active ? "Inativar" : "Ativar",
          destructive: customer.active,
        },
      ))
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
        <PageHeading
          title="Clientes"
          total={list.data?.totalElements}
          loading={list.loading}
          singular={filtered ? "cliente encontrado" : "cliente cadastrado"}
          plural={filtered ? "clientes encontrados" : "clientes cadastrados"}
        />
        {has("customers.create") && (
          <button
            className="customer-icon-label"
            disabled={!!pending}
            onClick={() => listReturn.open("/customers/new")}
          >
            <Plus size={17} />
            Novo cliente
          </button>
        )}
      </div>
      <section className="panel customer-filters" aria-label="Filtros de clientes">
        <label>
          Pesquisar por
          <FilterSelect
            value={list.filters.searchField}
            onChange={(event) => changeFilter("searchField", event.target.value)}
          >
            <option value="NAME">Nome</option>
            <option value="DOCUMENT">CPF/CNPJ</option>
            <option value="CONTACT">Contato</option>
          </FilterSelect>
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
          <FilterSelect
            value={list.filters.personType}
            onChange={(event) => changeFilter("personType", event.target.value)}
          >
            <option value="">Todos</option>
            <option value="PF">Pessoa física</option>
            <option value="PJ">Pessoa jurídica</option>
          </FilterSelect>
        </label>
        <label>
          Situação
          <FilterSelect
            value={list.filters.active}
            onChange={(event) => changeFilter("active", event.target.value)}
          >
            <option value="">Todas</option>
            <option value="true">Ativos</option>
            <option value="false">Inativos</option>
          </FilterSelect>
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
                      <DocumentValue
                        value={customer.taxId}
                        scope={[customer.id, list.page, list.size, list.filters]}
                        label={`CPF/CNPJ de ${customer.legalName}`}
                      />
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
                      <RowActions
                        className="customer-row-actions"
                        label={`Ações de ${customer.legalName}`}
                      >
                        <button
                          className="secondary icon-button"
                          aria-label={`Detalhes de ${customer.legalName}`}
                          title="Detalhes"
                          disabled={!!pending}
                          onClick={() => listReturn.open(`/customers/${customer.id}`)}
                        >
                          <Eye size={17} />
                        </button>
                        {has("customers.update") && (
                          <button
                            className="secondary icon-button"
                            aria-label={`Editar ${customer.legalName}`}
                            title="Editar"
                            disabled={!!pending}
                            onClick={() => listReturn.open(`/customers/${customer.id}/edit`)}
                          >
                            <Pencil size={17} />
                          </button>
                        )}
                        <button
                          className="secondary icon-button"
                          aria-label={`Endereços de ${customer.legalName}`}
                          title="Endereços"
                          disabled={!!pending}
                          onClick={() =>
                            listReturn.open(`/customers/${customer.id}?section=addresses`)
                          }
                        >
                          <MapPin size={17} />
                        </button>
                        {has("customers.update") && (
                          <button
                            className="secondary icon-button row-status-action"
                            aria-label={`${customer.active ? "Inativar" : "Ativar"} ${customer.legalName}`}
                            title={customer.active ? "Inativar" : "Ativar"}
                            disabled={!!pending}
                            onClick={() => changeActive(customer)}
                          >
                            <Power size={17} />
                          </button>
                        )}
                      </RowActions>
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
    </div>
  );
}
