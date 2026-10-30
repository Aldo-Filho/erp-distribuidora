import FilterSelect from "../../components/feedback/FilterSelect";
import PageHeading from "../../components/layout/PageHeading";
import { useCallback, useState } from "react";
import { security } from "../../services/security";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import {
  auditActions,
  auditOutcomes,
  auditActor,
  auditTarget,
  auditChanges,
} from "./securityAuditLabels";

export default function SecurityAuditPage() {
  const [fields, setFields] = useState({
    from: "",
    to: "",
    actorId: "",
    actorName: "",
    action: "",
    outcome: "",
  });
  const [filter, setFilter] = useState({});
  const [page, setPage] = useState(0);
  const loader = useCallback(() => security.audit({ ...filter, page }), [filter, page]);
  const { data, loading, error, reload } = useLoad(loader);
  const change = (key) => (event) => setFields({ ...fields, [key]: event.target.value });
  return (
    <>
      <div className="page-title">
        <PageHeading
          title="Auditoria de acesso"
          total={data?.totalElements}
          loading={loading}
          singular="evento encontrado"
          plural="eventos encontrados"
        />
      </div>
      <ErrorAlert message={error} retry={reload} />
      <section className="panel">
        <form
          className="filter-grid"
          onSubmit={(event) => {
            event.preventDefault();
            setFilter({
              ...fields,
              from: fields.from ? new Date(fields.from).toISOString() : "",
              to: fields.to ? new Date(fields.to).toISOString() : "",
            });
            setPage(0);
          }}
        >
          <label>
            De
            <input type="datetime-local" value={fields.from} onChange={change("from")} />
          </label>
          <label>
            Até
            <input type="datetime-local" value={fields.to} onChange={change("to")} />
          </label>
          <label>
            Nome do responsável
            <input value={fields.actorName} onChange={change("actorName")} />
          </label>
          <label>
            ID do responsável
            <input
              type="number"
              min="1"
              step="1"
              value={fields.actorId}
              onChange={change("actorId")}
            />
          </label>
          <label>
            Ação
            <FilterSelect value={fields.action} onChange={change("action")}>
              <option value="">Todas</option>
              {Object.entries(auditActions)
                .sort(([, first], [, second]) => first.localeCompare(second, "pt-BR"))
                .map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
            </FilterSelect>
          </label>
          <label>
            Resultado
            <FilterSelect value={fields.outcome} onChange={change("outcome")}>
              <option value="">Todos</option>
              {Object.entries(auditOutcomes).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </FilterSelect>
          </label>
          <button className="align-end">Filtrar</button>
        </form>
      </section>
      <section className="panel table-panel">
        {loading ? (
          <p role="status">Carregando eventos…</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Data e hora</th>
                  <th>Responsável</th>
                  <th>Ação</th>
                  <th>Alvo</th>
                  <th>Resultado</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((event) => {
                  const target = auditTarget(event);
                  const changes = auditChanges(event);
                  return (
                    <tr key={event.id}>
                      <td>{new Date(event.occurredAt).toLocaleString("pt-BR")}</td>
                      <td>
                        <strong>{auditActor(event)}</strong>
                        {event.actorId != null && <small>ID {event.actorId}</small>}
                      </td>
                      <td>
                        {auditActions[event.action] || "Ação não reconhecida"}
                        {changes && <small>{changes}</small>}
                      </td>
                      <td>
                        {target ? (
                          <>
                            <strong>{target.name || target.label}</strong>
                            <small className="identifier">
                              {target.label} · ID {event.target}
                            </small>
                          </>
                        ) : (
                          "—"
                        )}
                      </td>
                      <td>{auditOutcomes[event.outcome] || "Resultado não reconhecido"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {!data?.content.length && <p className="empty">Nenhum evento encontrado.</p>}
          </div>
        )}
        <Pagination page={page} totalPages={data?.totalPages} change={setPage} />
      </section>
    </>
  );
}
