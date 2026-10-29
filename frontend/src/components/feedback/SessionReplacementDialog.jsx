import { useState } from "react";
import ErrorAlert from "./ErrorAlert";

export default function SessionReplacementDialog({ challenge, replace, cancel }) {
  const [selected, setSelected] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const submit = async (event) => {
    event.preventDefault();
    setPending(true);
    setError("");
    try {
      await replace(selected);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending(false);
    }
  };
  return (
    <div className="modal-backdrop">
      <section
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="replacement-title"
      >
        <h2 id="replacement-title">Limite de sessões atingido</h2>
        <p>Escolha uma sessão para encerrar e entrar neste navegador.</p>
        <form onSubmit={submit}>
          <div className="session-options">
            {challenge.sessions.map((session) => (
              <label className="choice" key={session.id}>
                <input
                  type="radio"
                  name="session"
                  value={session.id}
                  checked={selected === session.id}
                  onChange={(event) => setSelected(event.target.value)}
                  required
                />
                <span>
                  <strong>{session.browser}</strong>
                  <small>
                    Entrada: {new Date(session.authenticatedAt).toLocaleString("pt-BR")}
                    <br />
                    Atividade: {new Date(session.lastActivityAt).toLocaleString("pt-BR")}
                  </small>
                </span>
              </label>
            ))}
          </div>
          <ErrorAlert message={error} />
          <div className="actions">
            <button type="button" className="secondary" disabled={pending} onClick={cancel}>
              Cancelar
            </button>
            <button disabled={pending || !selected}>
              {pending ? "Entrando…" : "Encerrar e entrar"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
