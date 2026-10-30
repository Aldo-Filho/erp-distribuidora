import useConfirm from "../../components/feedback/useConfirm";
import PageHeading from "../../components/layout/PageHeading";
import { useCallback, useState } from "react";
import { access } from "../../services/access";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RecordMetadata from "../../components/record/RecordMetadata";

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
  const confirm = useConfirm();
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
  const hasGrantedPermission = (key) =>
    (data?.permissions || []).some(
      (permission) => permission.permissionKey === key && grants.includes(permission.id),
    );
  const missingPurchaseRead =
    hasGrantedPermission("purchases.receive") && !hasGrantedPermission("purchases.read");
  const missingStockRead =
    hasGrantedPermission("stock.adjust") && !hasGrantedPermission("stock.read");
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
          !(await confirm(
            "Esta permissão permite conceder acesso amplo ao sistema, inclusive poderes administrativos. Conceder?",
            { title: "Conceder permissão", confirmLabel: "Conceder" },
          ))
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
        <PageHeading
          title="Cargos"
          total={data?.roles.length}
          loading={loading}
          singular="cargo cadastrado"
          plural="cargos cadastrados"
        />
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
                            !(await confirm(
                              "Excluir este cargo? Vínculos com usuários e permissões impedem a exclusão.",
                              {
                                title: "Confirmar exclusão",
                                confirmLabel: "Excluir",
                                destructive: true,
                              },
                            ))
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
                {selected &&
                  !creating &&
                  !loading &&
                  !error &&
                  data?.roles.some((role) => role.id === selected) && (
                    <RecordMetadata
                      record={data.roles.find((role) => role.id === selected)}
                      fields={["createdAt"]}
                    />
                  )}
              </section>
              {selected && (
                <section className="panel">
                  <h2>Permissões</h2>
                  <p className="field-hint">
                    Cada alteração é salva ao marcar ou desmarcar. Consultas de produtos e
                    fornecedores incluem custos. O acesso aos registros é geral dentro das funções
                    liberadas.
                  </p>
                  {missingPurchaseRead && (
                    <p className="product-warning" role="status">
                      O cargo possui a permissão Receber compras, mas não Consultar compras. A tela
                      de compras e a conferência de recebimentos exigem Consultar compras. Conceda
                      também essa permissão para permitir o recebimento pela interface.
                    </p>
                  )}
                  {missingStockRead && (
                    <p className="product-warning" role="status">
                      O cargo possui a permissão Movimentar / ajustar estoque, mas não Consultar
                      estoque. A tela de estoque e a verificação do histórico exigem Consultar
                      estoque. Conceda também essa permissão para utilizar o ajuste pela interface.
                    </p>
                  )}
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