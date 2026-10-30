import RowActions from "../../components/feedback/RowActions";
import DocumentValue from "../../components/feedback/DocumentValue";
import useConfirm from "../../components/feedback/useConfirm";
import PageHeading from "../../components/layout/PageHeading";
import FilterSelect from "../../components/feedback/FilterSelect";
import { useEffect, useRef, useState } from "react";
import { Eye, MapPin, Pencil, Plus, Power } from "lucide-react";
import useAuth from "../../auth/useAuth";
import { suppliers } from "../../services/suppliers";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import Modal from "../../components/feedback/Modal";
import SupplierForm from "./SupplierForm.jsx";
import SupplierDetailsModal from "./SupplierDetailsModal";
import SupplierAddressModal from "./SupplierAddressModal";
import SupplierCatalogLinksModal from "./SupplierCatalogLinksModal";
import useSuppliers, { emptyFilters } from "./useSuppliers";

function EditSupplier({ supplierId, onClose, onSaved }) {
  const [state, setState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setState({ loading: true });
    suppliers.findOne(supplierId, { signal: controller.signal }).then(
      (supplier) => {
        if (live) setState({ supplier });
      },
      (issue) => {
        if (live) setState({ error: issue.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [supplierId, revision]);
  if (state.supplier)
    return (
      <SupplierForm
        key={state.supplier.id}
        supplier={state.supplier}
        onClose={onClose}
        onSaved={onSaved}
      />
    );
  return (
    <Modal title="Editar fornecedor" onClose={onClose}>
      <ErrorAlert message={state.error} retry={() => setRevision((number) => number + 1)} />
      {state.loading ? (
        <p role="status">Carregando fornecedor…</p>
      ) : (
        !state.error && <p>Fornecedor não encontrado.</p>
      )}
    </Modal>
  );
}

export default function SuppliersPage() {
  const confirm = useConfirm();
  const { has, session } = useAuth();
  const list = useSuppliers();
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
    setNotice("Fornecedor salvo.");
    list.reload();
    closeFlow();
  };
  const changeActive = async (supplier) => {
    if (lock.current || !has("suppliers.update")) return;
    const action = supplier.active ? "Inativar" : "Ativar";
    if (
      !(await confirm(`${action} o fornecedor ${supplier.legalName}?`, {
        title: `${action} fornecedor`,
        confirmLabel: action,
      }))
    )
      return;
    lock.current = true;
    setPending(supplier.id);
    setFailure("");
    setNotice("");
    try {
      const updated = await suppliers.update(supplier.id, { active: !supplier.active });
      if (live.current) {
        setNotice(updated.active ? "Fornecedor ativado." : "Fornecedor inativado.");
        list.reload();
      }
    } catch (issue) {
      if (live.current) setFailure(issue.message);
    } finally {
      lock.current = false;
      if (live.current) setPending(null);
    }
  };
  const filtered = !!(list.filters.query.trim() || list.filters.active !== "");
  return (
    <div className="customers-page">
      <div className="page-title">
        <PageHeading
          title="Fornecedores"
          total={list.data?.totalElements}
          loading={list.loading}
          singular={filtered ? "fornecedor encontrado" : "fornecedor cadastrado"}
          plural={filtered ? "fornecedores encontrados" : "fornecedores cadastrados"}
        />
        {has("suppliers.create") && (
          <button
            className="customer-icon-label"
            disabled={!!pending}
            onClick={() => setFlow({ kind: "new" })}
          >
            <Plus size={17} />
            Novo fornecedor
          </button>
        )}
      </div>
      <section className="panel customer-filters" aria-label="Filtros de fornecedores">
        <label>
          Pesquisar por
          <FilterSelect
            value={list.filters.searchField}
            onChange={(event) => changeFilter("searchField", event.target.value)}
          >
            <option value="NAME">Nome</option>
            <option value="DOCUMENT">CNPJ</option>
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
                ? "Razão social ou nome fantasia"
                : list.filters.searchField === "DOCUMENT"
                  ? "CNPJ"
                  : "E-mail ou telefone"
            }
            onChange={(event) => changeFilter("query", event.target.value)}
          />
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
        aria-label="Lista de fornecedores"
        aria-busy={list.loading}
      >
        {list.loading ? (
          <p role="status">Carregando fornecedores…</p>
        ) : list.error ? (
          <p>Não foi possível carregar os fornecedores.</p>
        ) : !list.data?.content.length ? (
          <p className="empty">
            {filtered
              ? "Nenhum fornecedor encontrado para estes filtros."
              : "Nenhum fornecedor cadastrado."}
          </p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Fornecedor</th>
                  <th>
                    CNPJ
                  </th>
                  <th>Contato</th>
                  <th>Situação</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {list.data.content.map((supplier) => (
                  <tr key={supplier.id}>
                    <td>
                      <strong>{supplier.legalName}</strong>
                      {supplier.tradeName && <small>{supplier.tradeName}</small>}
                    </td>
                    <td>
                      <DocumentValue
                        value={supplier.taxId}
                        scope={[supplier.id, list.page, list.size, list.filters]}
                        label={`CNPJ de ${supplier.legalName}`}
                      />
                    </td>
                    <td>
                      {supplier.phone || "Telefone não informado"}
                      <small>{supplier.email}</small>
                      {supplier.whatsapp && <small>WhatsApp: {supplier.whatsapp}</small>}
                    </td>
                    <td>
                      <span className={"badge " + (supplier.active ? "active" : "")}>
                        {supplier.active ? "Ativo" : "Inativo"}
                      </span>
                    </td>
                    <td>
                      <RowActions
                        className="customer-row-actions"
                        label={`Ações de ${supplier.legalName}`}
                      >
                        <button
                          className="secondary icon-button"
                          aria-label={`Detalhes de ${supplier.legalName}`}
                          title="Detalhes"
                          disabled={!!pending}
                          onClick={() => setFlow({ kind: "details", id: supplier.id })}
                        >
                          <Eye size={17} />
                        </button>
                        {has("suppliers.update") && (
                          <button
                            className="secondary icon-button"
                            aria-label={`Editar ${supplier.legalName}`}
                            title="Editar"
                            disabled={!!pending}
                            onClick={() => setFlow({ kind: "edit", id: supplier.id })}
                          >
                            <Pencil size={17} />
                          </button>
                        )}
                        <button
                          className="secondary icon-button"
                          aria-label={`Endereços de ${supplier.legalName}`}
                          title="Endereços"
                          disabled={!!pending}
                          onClick={() => setFlow({ kind: "addresses", id: supplier.id })}
                        >
                          <MapPin size={17} />
                        </button>
                        {has("suppliers.update") && (
                          <button
                            className="secondary icon-button row-status-action"
                            aria-label={`${supplier.active ? "Inativar" : "Ativar"} ${supplier.legalName}`}
                            title={supplier.active ? "Inativar" : "Ativar"}
                            disabled={!!pending}
                            onClick={() => changeActive(supplier)}
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
              ? `${list.data.totalElements} ${
                  list.data.totalElements === 1 ? "fornecedor" : "fornecedores"
                }`
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
      {flow?.kind === "new" && has("suppliers.create") && (
        <SupplierForm
          key={`new:${session.userId}`}
          supplier={null}
          onClose={closeFlow}
          onSaved={saved}
        />
      )}
      {flow?.kind === "edit" && has("suppliers.update") && (
        <EditSupplier
          key={`${session.userId}:${flow.id}`}
          supplierId={flow.id}
          onClose={closeFlow}
          onSaved={saved}
        />
      )}
      {flow?.kind === "details" && (
        <SupplierDetailsModal
          key={`details:${flow.id}`}
          supplierId={flow.id}
          onClose={closeFlow}
          onEdit={(supplier) => setFlow({ kind: "edit", id: supplier.id, returnTo: supplier.id })}
          onAddresses={(id) => setFlow({ kind: "addresses", id, returnTo: id })}
          onCatalogLinks={(id) => setFlow({ kind: "catalogLinks", id, returnTo: id })}
        />
      )}
      {flow?.kind === "addresses" && (
        <SupplierAddressModal
          key={`addresses:${flow.id}`}
          supplierId={flow.id}
          onClose={closeFlow}
        />
      )}
      {flow?.kind === "catalogLinks" && has("suppliers.read") && (
        <SupplierCatalogLinksModal key={`links:${flow.id}`} supplierId={flow.id} onClose={closeFlow} />
      )}
    </div>
  );
}
