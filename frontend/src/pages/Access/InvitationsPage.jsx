import { useCallback, useState } from "react";
import { access } from "../../services/access";
import { useDraft } from "../../auth/drafts";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";

export default function InvitationsPage() {
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [search, setSearch] = useState("");
  const [name, setName] = useDraft("invitation-name", "");
  const [email, setEmail] = useDraft("invitation-email", "");
  const [roleId, setRoleId] = useDraft("invitation-role", "");
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState("");
  const [notice, setNotice] = useState("");
  const loader = useCallback(
    async () => ({
      invitations: await access.invitations(page, search),
      roles: await access.roles(),
    }),
    [page, search],
  );
  const { data, loading, error, reload } = useLoad(loader);
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
      <div className="page-title">
        <h1>Convites</h1>
      </div>
      <ErrorAlert message={failure || error} retry={error ? reload : undefined} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <section className="panel">
        <h2>Convidar funcionário</h2>
        <form
          className="form-grid"
          onSubmit={(event) => {
            event.preventDefault();
            run(async () => {
              await access.invite({ name, email, roleId: Number(roleId) });
              setName("");
              setEmail("");
              setRoleId("");
              setNotice("Convite criado e envio solicitado. Confira o estado de envio na lista.");
            });
          }}
        >
          <label>
            Nome
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label>
            E-mail
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>
          <label>
            Cargo
            <select value={roleId} onChange={(event) => setRoleId(event.target.value)} required>
              <option value="">Selecionar cargo</option>
              {data?.roles
                .filter((role) => role.active)
                .map((role) => (
                  <option key={role.id} value={role.id}>
                    {role.name}
                  </option>
                ))}
            </select>
          </label>
          <div className="actions align-end">
            <button disabled={pending || !data}>
              {pending ? "Solicitando…" : "Enviar convite"}
            </button>
          </div>
        </form>
      </section>
      <section className="panel table-panel">
        <form
          className="list-toolbar"
          onSubmit={(event) => {
            event.preventDefault();
            setSearch(query);
            setPage(0);
          }}
        >
          <input
            type="search"
            aria-label="Filtrar convites por e-mail"
            placeholder="Filtrar por e-mail"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button className="secondary">Buscar</button>
        </form>
        {loading ? (
          <p role="status">Carregando convites…</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Funcionário</th>
                  <th>Validade</th>
                  <th>Estado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {data?.invitations.content.map((invitation) => {
                  const closed = invitation.consumedAt || invitation.canceledAt;
                  const state = invitation.consumedAt
                    ? "Aceito"
                    : invitation.canceledAt
                      ? "Cancelado"
                      : Date.parse(invitation.expiresAt) < Date.now()
                        ? "Expirado"
                        : "Pendente";
                  const delivery = {
                    PENDING: "Envio pendente",
                    SUBMITTED: "Submetido ao SMTP",
                    FAILED: "Falha no envio",
                  }[invitation.deliveryStatus];
                  return (
                    <tr key={invitation.id}>
                      <td>
                        <strong>{invitation.name}</strong>
                        <small>{invitation.email}</small>
                      </td>
                      <td>{new Date(invitation.expiresAt).toLocaleString("pt-BR")}</td>
                      <td>
                        {state}
                        <small>{delivery}</small>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            className="secondary"
                            disabled={pending || !!closed}
                            onClick={() =>
                              run(async () => {
                                await access.resend(invitation.id);
                                setNotice("Reenvio solicitado. O link anterior foi invalidado.");
                              })
                            }
                          >
                            Reenviar
                          </button>
                          <button
                            className="danger-link"
                            disabled={pending || !!closed}
                            onClick={() => {
                              if (window.confirm("Cancelar o convite de " + invitation.email + "?"))
                                run(() => access.cancelInvite(invitation.id));
                            }}
                          >
                            Cancelar
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {!data?.invitations.content.length && (
              <p className="empty">Nenhum convite encontrado.</p>
            )}
          </div>
        )}
        <Pagination page={page} totalPages={data?.invitations.totalPages} change={setPage} />
      </section>
    </>
  );
}
