import { useCallback, useState } from "react";
import { access } from "../../services/access";
import { useDraft } from "../../auth/drafts";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import UserDetailsModal from "./UserDetailsModal";

export default function UsersPage() {
  const loader = useCallback(
    async () => ({ users: await access.users(), roles: await access.roles() }),
    [],
  );
  const { data, loading, error, reload } = useLoad(loader);
  const [editing, setEditing] = useDraft("user-edit", null);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState("");
  const [notice, setNotice] = useState("");
  const [filter, setFilter] = useState("");
  const [detailsId, setDetailsId] = useState(null);
  const run = async (action) => {
    setPending(true);
    setFailure("");
    setNotice("");
    try {
      await action();
      await reload();
    } catch (issue) {
      setFailure(issue.message);
    } finally {
      setPending(false);
    }
  };
  return (
    <>
      {detailsId && (
        <UserDetailsModal
          key={detailsId}
          userId={detailsId}
          onClose={() => setDetailsId(null)}
        />
      )}
      <div className="page-title">
        <h1>Funcionários</h1>
        <input
          className="search"
          aria-label="Buscar funcionário"
          placeholder="Buscar nome ou e-mail"
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
        />
      </div>
      <ErrorAlert message={failure || error} retry={error ? reload : undefined} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      {editing && (
        <section className="panel">
          <h2>Editar funcionário</h2>
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();
              run(async () => {
                await access.updateUser(editing.id, {
                  name: editing.name,
                  email: editing.email,
                  roleId: Number(editing.roleId),
                  active: editing.active,
                });
                setEditing(null);
              });
            }}
          >
            <label>
              Nome
              <input
                required
                value={editing.name}
                onChange={(event) => setEditing({ ...editing, name: event.target.value })}
              />
            </label>
            <label>
              E-mail
              <input
                type="email"
                required
                value={editing.email}
                onChange={(event) => setEditing({ ...editing, email: event.target.value })}
              />
            </label>
            <label>
              Cargo
              <select
                required
                value={editing.roleId}
                onChange={(event) => setEditing({ ...editing, roleId: event.target.value })}
              >
                {data?.roles.map((role) => (
                  <option key={role.id} value={role.id}>
                    {role.name}
                    {!role.active ? " (inativo)" : ""}
                  </option>
                ))}
              </select>
            </label>
            <label className="check">
              <input
                type="checkbox"
                checked={editing.active}
                onChange={(event) => setEditing({ ...editing, active: event.target.checked })}
              />
              Ativo
            </label>
            <p className="field-hint span-all">
              Alterar o e-mail ou inativar encerra os acessos atuais. Para definir senha, use a
              recuperação por e-mail.
            </p>
            <div className="actions span-all">
              <button
                type="button"
                className="secondary"
                disabled={pending}
                onClick={() => setEditing(null)}
              >
                Cancelar
              </button>
              <button disabled={pending}>{pending ? "Salvando…" : "Salvar"}</button>
            </div>
          </form>
        </section>
      )}
      <section className="panel table-panel">
        {loading ? (
          <p role="status">Carregando funcionários…</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Funcionário</th>
                  <th>Cargo</th>
                  <th>Status</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {data?.users
                  .filter((user) =>
                    (user.name + " " + user.email)
                      .toLocaleLowerCase()
                      .includes(filter.toLocaleLowerCase()),
                  )
                  .map((user) => (
                    <tr key={user.id}>
                      <td>
                        <strong>{user.name}</strong>
                        <small>{user.email}</small>
                      </td>
                      <td>{user.role?.name}</td>
                      <td>
                        <span className={"badge " + (user.active ? "active" : "")}>
                          {user.active ? "Ativo" : "Inativo"}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            className="secondary"
                            disabled={pending}
                            onClick={() => setDetailsId(user.id)}
                          >
                            Detalhes
                          </button>
                          <button
                            className="secondary"
                            disabled={pending}
                            onClick={() => {
                              setEditing({
                                id: user.id,
                                name: user.name,
                                email: user.email,
                                roleId: user.role.id,
                                active: user.active,
                              });
                              setFailure("");
                            }}
                          >
                            Editar
                          </button>
                          <button
                            className="secondary"
                            disabled={pending}
                            onClick={() =>
                              run(async () => {
                                await access.recoverUser(user.id);
                                setNotice(
                                  "Solicitação de recuperação registrada. A entrega depende do serviço de e-mail.",
                                );
                              })
                            }
                          >
                            Recuperar acesso
                          </button>
                          <button
                            className="danger-link"
                            disabled={pending}
                            onClick={() => {
                              if (
                                window.confirm(
                                  "Excluir " +
                                    user.name +
                                    "? Registros históricos podem impedir a exclusão.",
                                )
                              )
                                run(() => access.deleteUser(user.id));
                            }}
                          >
                            Excluir
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
            {data?.users.length === 0 && <p className="empty">Nenhum funcionário cadastrado.</p>}
          </div>
        )}
      </section>
    </>
  );
}
