import { useState } from "react";
import useAuth from "../../auth/useAuth";
import { auth } from "../../services/auth";
import { canAccessEntry, navigation } from "../../routes/navigation";
import ErrorAlert from "../../components/feedback/ErrorAlert";

export default function AccountPage() {
  const { session, signedOut } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const available = navigation.some(
    (entry) => (entry.permission || entry.anyPermissions) && canAccessEntry(entry, session),
  );
  return (
    <>
      <div className="page-title">
        <h1>Minha conta</h1>
      </div>
      {!available && (
        <p className="notice">
          Seu cargo ainda não tem telas de módulos disponíveis nesta entrega. Você pode administrar
          sua conta e suas sessões.
        </p>
      )}
      <section className="panel narrow">
        <h2>{session.name}</h2>
        <dl className="account-details">
          <dt>E-mail</dt>
          <dd>{session.email}</dd>
          <dt>Cargo</dt>
          <dd>{session.roleName}</dd>
        </dl>
      </section>
      <section className="panel narrow">
        <h2>Alterar senha</h2>
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            setError("");
            if (Array.from(newPassword).length < 15 || Array.from(newPassword).length > 128) {
              setError("Use de 15 a 128 caracteres.");
              return;
            }
            if (newPassword !== confirmation) {
              setError("As senhas não coincidem.");
              return;
            }
            setPending(true);
            try {
              await auth.password({ currentPassword, newPassword });
              setCurrentPassword("");
              setNewPassword("");
              setConfirmation("");
              signedOut();
            } catch (failure) {
              setError(failure.message);
            } finally {
              setPending(false);
            }
          }}
        >
          <label>
            Senha atual
            <input
              type="password"
              required
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </label>
          <label>
            Nova senha
            <input
              type="password"
              required
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
          </label>
          <label>
            Confirmar nova senha
            <input
              type="password"
              required
              autoComplete="new-password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
            />
          </label>
          <p className="field-hint">
            Use entre 15 e 128 caracteres. A alteração encerra todas as suas sessões e exige novo
            login.
          </p>
          <ErrorAlert message={error} />
          <div className="actions">
            <button disabled={pending}>{pending ? "Alterando…" : "Alterar senha e sair"}</button>
          </div>
        </form>
      </section>
    </>
  );
}
