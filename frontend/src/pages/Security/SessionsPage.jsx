import useConfirm from "../../components/feedback/useConfirm";
import PageHeading from "../../components/layout/PageHeading";
import FilterSelect from "../../components/feedback/FilterSelect";
import { useCallback, useState } from "react";
import useAuth from "../../auth/useAuth";
import { access } from "../../services/access";
import { security } from "../../services/security";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";

export default function SessionsPage() {
  const confirm = useConfirm();
  const { session, has, refresh } = useAuth();
  const [userId, setUserId] = useState("");
  const [page, setPage] = useState(0);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState("");
  const manage = has("security.sessions.manage");
  const loader = useCallback(
    async () => {
      const response = await security.sessions(userId, page);
      return {
        sessions: userId ? response.content : response,
        totalElements: userId ? response.totalElements : response.length,
        totalPages: userId ? response.totalPages : 0,
        users: manage ? await access.users() : [],
      };
    },
    [userId, page, manage],
  );
  const { data, loading, error, reload } = useLoad(loader);
  const sessions = data?.sessions;
  return (
    <>
      <div className="page-title">
        <PageHeading
          title="Sessões"
          total={data?.totalElements}
          loading={loading}
          singular="sessão encontrada"
          plural="sessões encontradas"
        />
        <div className="row-actions">
          {manage && (
            <FilterSelect
              aria-label="Funcionário"
              value={userId}
              onChange={(event) => {
                setUserId(event.target.value);
                setPage(0);
              }}
            >
              <option value="">Minhas sessões</option>
              {data?.users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name}
                </option>
              ))}
            </FilterSelect>
          )}
          <button className="secondary" onClick={reload}>
            Atualizar
          </button>
        </div>
      </div>
      <ErrorAlert message={failure || error} retry={error ? reload : undefined} />
      <section className="panel table-panel">
        {loading ? (
          <p role="status">Carregando sessões…</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Navegador</th>
                  <th>Entrada</th>
                  <th>Última atividade</th>
                  <th>Ação</th>
                </tr>
              </thead>
              <tbody>
                {sessions?.map((entry) => (
                  <tr key={entry.id}>
                    <td>
                      <strong>{entry.browser}</strong>
                      {entry.id === session.publicSessionId && <small>Esta sessão</small>}
                    </td>
                    <td>{new Date(entry.authenticatedAt).toLocaleString("pt-BR")}</td>
                    <td>{new Date(entry.lastActivityAt).toLocaleString("pt-BR")}</td>
                    <td>
                      <button
                        className="danger-link"
                        disabled={pending}
                        onClick={async () => {
                          if (
                            !(await confirm(
                              entry.id === session.publicSessionId
                                ? "Encerrar esta sessão? Você precisará entrar novamente."
                                : "Encerrar a sessão selecionada?",
                              {
                                title: "Encerrar sessão",
                                confirmLabel: "Encerrar sessão",
                                destructive: true,
                              },
                            ))
                          )
                            return;
                          setPending(true);
                          setFailure("");
                          try {
                            await security.revoke(entry.id, userId);
                            await refresh();
                            await reload();
                          } catch (issue) {
                            setFailure(issue.message);
                          } finally {
                            setPending(false);
                          }
                        }}
                      >
                        Encerrar sessão
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!sessions?.length && <p className="empty">Nenhuma sessão ativa.</p>}
          </div>
        )}
        {userId && (
          <Pagination page={page} totalPages={data?.totalPages} change={setPage} />
        )}
      </section>
    </>
  );
}
