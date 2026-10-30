import useConfirm from "../../components/feedback/useConfirm";
import { useCallback, useEffect, useState } from "react";
import { security } from "../../services/security";
import useAuth from "../../auth/useAuth";
import useLoad from "../../components/feedback/useLoad";
import ErrorAlert from "../../components/feedback/ErrorAlert";

export default function SecuritySettingsPage() {
  const confirm = useConfirm();
  const loader = useCallback(security.settings, []);
  const { data, loading, error, reload } = useLoad(loader);
  const { refresh } = useAuth();
  const [form, setForm] = useState(null);
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState("");
  const [notice, setNotice] = useState("");
  useEffect(() => {
    if (data) setForm(data);
  }, [data]);
  const field = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  return (
    <>
      <div className="page-title">
        <h1>Política de acesso</h1>
      </div>
      <ErrorAlert message={failure || error} retry={error ? reload : undefined} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <section className="panel narrow">
        {loading || !form ? (
          <p role="status">Carregando política…</p>
        ) : (
          <form
            onSubmit={async (event) => {
              event.preventDefault();
              setFailure("");
              setNotice("");
              const values = {
                idleMinutes: Number(form.idleMinutes),
                absoluteMinutes: Number(form.absoluteMinutes),
                maxSessions: Number(form.maxSessions),
              };
              if (
                !Object.values(values).every(Number.isInteger) ||
                values.idleMinutes > values.absoluteMinutes
              ) {
                setFailure(
                  "A inatividade não pode exceder a duração máxima. Use minutos inteiros.",
                );
                return;
              }
              if (
                !(await confirm(
                  "Aplicar " +
                    values.idleMinutes +
                    " min de inatividade, " +
                    values.absoluteMinutes +
                    " min de duração e " +
                    values.maxSessions +
                    " sessão(ões)? Sessões fora dos novos prazos serão encerradas. Excedentes serão revogados pela menor atividade, preservando esta sessão quando possível. Aumentar os limites não recupera sessões encerradas.",
                  { title: "Aplicar política de acesso", confirmLabel: "Aplicar política" },
                ))
              )
                return;
              setPending(true);
              try {
                setForm(await security.updateSettings(values));
                setNotice("Política atualizada.");
                await refresh();
              } catch (issue) {
                setFailure(issue.message);
              } finally {
                setPending(false);
              }
            }}
          >
            <label>
              Inatividade (minutos)
              <input
                type="number"
                min="15"
                max="240"
                step="1"
                required
                value={form.idleMinutes}
                onChange={field("idleMinutes")}
              />
              <small>De 15 a 240 minutos.</small>
            </label>
            <label>
              Duração máxima (minutos)
              <input
                type="number"
                min="60"
                max="1440"
                step="1"
                required
                value={form.absoluteMinutes}
                onChange={field("absoluteMinutes")}
              />
              <small>De 60 a 1.440 minutos. Atividade não amplia este prazo.</small>
            </label>
            <label>
              Sessões simultâneas por funcionário
              <input
                type="number"
                min="1"
                max="5"
                step="1"
                required
                value={form.maxSessions}
                onChange={field("maxSessions")}
              />
            </label>
            <p className="field-hint">
              A política passa a valer também para as sessões existentes. Limites de tentativas e
              envio de e-mail são configurados pela implantação.
            </p>
            <div className="actions">
              <button disabled={pending}>{pending ? "Aplicando…" : "Revisar e aplicar"}</button>
            </div>
          </form>
        )}
      </section>
    </>
  );
}
