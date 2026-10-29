import { useCallback, useState } from "react";
import { access } from "../../services/access";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";

const modules = {
  customers: "Clientes",
  suppliers: "Fornecedores",
  products: "Produtos",
  warehouses: "Depósitos",
  stock: "Estoque",
  prices: "Preços",
  orders: "Pedidos",
  purchases: "Compras",
  "access.users": "Funcionários",
  "access.roles": "Cargos e permissões",
  "security.settings": "Política de acesso",
  "security.sessions": "Sessões de terceiros",
  "security.audit": "Auditoria",
};
const actions = {
  read: "Consultar",
  create: "Cadastrar",
  update: "Editar",
  delete: "Excluir",
  manage: "Administrar",
  configure: "Configurar saldos",
  adjust: "Movimentar / ajustar",
  confirm: "Confirmar",
  cancel: "Cancelar",
  send: "Enviar",
  receive: "Receber",
  dispatch: "Dar saída",
  manualPrice: "Informar preço manual",
  discount: "Aplicar / alterar desconto",
};

export default function RolesPage() {
  const loader = useCallback(
    async () => ({ roles: await access.roles(), permissions: await access.permissions() }),
    [],
  );
  const { data, loading, error, reload } = useLoad(loader);
  const [selected, setSelected] = useState(null);
  const [grants, setGrants] = useState([]);
  const [form, setForm] = useState({ name: "", description: "", active: true });
  const [creating, setCreating] = useState(false);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState("");
  const select = async (role) => {
    setPending(true);
    setFailure("");
    try {
      const values = await access.grants(role.id);
      setSelected(role.id);
      setCreating(false);
      setForm({ name: role.name, description: role.description || "", active: role.active });
      setGrants(values.map((grant) => grant.permission.id));
    } catch (issue) {
      setFailure(issue.message);
    } finally {
      setPending(false);
    }
  };
  const groups = Object.groupBy(data?.permissions || [], (permission) => permission.module);
  const toggle = async (permission) => {
    setPending(true);
    setFailure("");
    try {
      if (grants.includes(permission.id)) {
        await access.revoke(selected, permission.id);
        setGrants((values) => values.filter((id) => id !== permission.id));
      } else {
        if (
          permission.permissionKey === "access.roles.manage" &&
          !window.confirm(
            "Esta permissão permite conceder acesso amplo ao sistema, inclusive poderes administrativos. Conceder?",
          )
        )
          return;
        await access.grant(selected, permission.id);
        setGrants((values) => [...values, permission.id]);
      }
    } catch (issue) {
      setFailure(issue.message);
    } finally {
      setPending(false);
    }
  };
  return (
    <>
      <div className="page-title">
        <h1>Cargos</h1>
        <button
          disabled={pending}
          onClick={() => {
            setCreating(true);
            setSelected(null);
            setGrants([]);
            setForm({ name: "", description: "", active: true });
          }}
        >
          Novo cargo
        </button>
      </div>
      <ErrorAlert message={failure || error} retry={error ? reload : undefined} />
      <div className="master-detail">
        <section className="panel role-list">
          {loading ? (
            <p role="status">Carregando cargos…</p>
          ) : (
            data?.roles.map((role) => (
              <button
                className={selected === role.id ? "role-choice selected" : "role-choice"}
                key={role.id}
                disabled={pending}
                onClick={() => select(role)}
              >
                <strong>{role.name}</strong>
                <small>{role.active ? "Ativo" : "Inativo"}</small>
              </button>
            ))
          )}
        </section>
        <div>
          {selected || creating ? (
            <>
              <section className="panel">
                <h2>{creating ? "Novo cargo" : "Configuração do cargo"}</h2>
                <form
                  className="form-grid"
                  onSubmit={async (event) => {
                    event.preventDefault();
                    setPending(true);
                    setFailure("");
                    try {
                      const role = creating
                        ? await access.createRole(form)
                        : await access.updateRole(selected, form);
                      await reload();
                      await select(role);
                    } catch (issue) {
                      setFailure(issue.message);
                    } finally {
                      setPending(false);
                    }
                  }}
                >
                  <label>
                    Nome
                    <input
                      required
                      value={form.name}
                      onChange={(event) => setForm({ ...form, name: event.target.value })}
                    />
                  </label>
                  <label>
                    Descrição
                    <input
                      value={form.description}
                      onChange={(event) => setForm({ ...form, description: event.target.value })}
                    />
                  </label>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={(event) => setForm({ ...form, active: event.target.checked })}
                    />
                    Ativo
                  </label>
                  <div className="actions">
                    <button disabled={pending}>{pending ? "Salvando…" : "Salvar cargo"}</button>
                    {selected && (
                      <button
                        type="button"
                        className="danger-link"
                        disabled={pending}
                        onClick={async () => {
                          if (
                            !window.confirm(
                              "Excluir este cargo? Vínculos com usuários e permissões impedem a exclusão.",
                            )
                          )
                            return;
                          setPending(true);
                          setFailure("");
                          try {
                            await access.deleteRole(selected);
                            setSelected(null);
                            await reload();
                          } catch (issue) {
                            setFailure(issue.message);
                          } finally {
                            setPending(false);
                          }
                        }}
                      >
                        Excluir
                      </button>
                    )}
                  </div>
                </form>
              </section>
              {selected && (
                <section className="panel">
                  <h2>Permissões</h2>
                  <p className="field-hint">
                    Cada alteração é salva ao marcar ou desmarcar. Consultas de produtos e
                    fornecedores incluem custos. O acesso aos registros é geral dentro das funções
                    liberadas.
                  </p>
                  {Object.entries(groups).map(([module, permissions]) => (
                    <fieldset className="permission-group" key={module}>
                      <legend>{modules[module] || module}</legend>
                      <div className="permission-grid">
                        {permissions.map((permission) => (
                          <label className="check" key={permission.id}>
                            <input
                              type="checkbox"
                              disabled={pending}
                              checked={grants.includes(permission.id)}
                              onChange={() => toggle(permission)}
                            />
                            {actions[permission.action] || permission.action}
                          </label>
                        ))}
                      </div>
                    </fieldset>
                  ))}
                </section>
              )}
            </>
          ) : (
            <section className="panel empty">
              Selecione um cargo para configurar suas permissões.
            </section>
          )}
        </div>
      </div>
    </>
  );
}
