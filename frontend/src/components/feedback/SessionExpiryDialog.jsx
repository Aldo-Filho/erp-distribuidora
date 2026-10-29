import { useEffect, useState } from "react";
import { auth } from "../../services/auth";
import ErrorAlert from "./ErrorAlert";

export default function SessionExpiryDialog({ session, refresh, logout }) {
  const [now, setNow] = useState(Date.now());
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const [clock, setClock] = useState({ at: Date.now(), server: Date.parse(session.serverTime) });
  useEffect(() => {
    setClock({ at: Date.now(), server: Date.parse(session.serverTime) });
  }, [session.serverTime]);
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, []);
  const remaining =
    Math.min(Date.parse(session.idleExpiresAt), Date.parse(session.absoluteExpiresAt)) -
    clock.server -
    (now - clock.at);
  const absolute = Date.parse(session.absoluteExpiresAt) - clock.server - (now - clock.at);
  if (remaining > 300000) return null;
  const continueSession = async () => {
    setPending(true);
    try {
      await auth.activity();
      await refresh();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending(false);
    }
  };
  return (
    <div className="modal-backdrop">
      <section className="dialog" role="dialog" aria-modal="true" aria-labelledby="expiry-title">
        <h2 id="expiry-title">Sua sessão está terminando</h2>
        <p>
          {remaining > 0
            ? "Tempo restante: " + Math.max(0, Math.ceil(remaining / 60000)) + " min."
            : "O prazo da sessão terminou. Verifique o acesso para continuar."}
        </p>
        <p>
          Conclua ou copie os dados ainda não salvos. Senhas e formulários não são restaurados após
          recarregar a página.
        </p>
        {absolute <= 300000 && <p>O limite total exige um novo login, mesmo com atividade.</p>}
        <ErrorAlert message={error} />
        <div className="actions">
          <button
            className="secondary"
            disabled={pending}
            onClick={async () => {
              try {
                await logout();
              } catch (failure) {
                setError(failure.message);
              }
            }}
          >
            Sair
          </button>
          <button disabled={pending} onClick={remaining <= 0 ? refresh : continueSession}>
            {pending ? "Verificando…" : remaining <= 0 ? "Verificar acesso" : "Continuar"}
          </button>
        </div>
      </section>
    </div>
  );
}
