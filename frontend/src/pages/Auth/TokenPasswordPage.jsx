import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { auth } from "../../services/auth";
import AuthLayout from "./AuthLayout";
import ErrorAlert from "../../components/feedback/ErrorAlert";

// Capture once before rendering and clear location so tokens never enter future navigation.
const tokenByPath = new Map();
function captureToken() {
  const path = window.location.pathname;
  const token =
    new URLSearchParams(window.location.hash.slice(1)).get("token") ||
    new URLSearchParams(window.location.search).get("token");
  if (token) {
    tokenByPath.set(path, token);
    window.history.replaceState(null, "", path);
  }
  return tokenByPath.get(path) || "";
}

export default function TokenPasswordPage({ invitation = false }) {
  const [token, setToken] = useState(captureToken);
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    const path = window.location.pathname;
    return () => {
      tokenByPath.delete(path);
    };
  }, []);
  return (
    <AuthLayout title={invitation ? "Definir senha" : "Redefinir senha"} showLogo={invitation}>
      {done ? (
        <p className="success" role="status">
          Senha definida. <Link to="/login">Entre na sua conta.</Link>
        </p>
      ) : !token ? (
        <p>Este link não contém um token válido. Solicite um novo link.</p>
      ) : (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            setError("");
            if (Array.from(password).length < 15 || Array.from(password).length > 128) {
              setError("Use entre 15 e 128 caracteres.");
              return;
            }
            if (password !== confirmation) {
              setError("As senhas não coincidem.");
              return;
            }
            setPending(true);
            try {
              if (invitation) await auth.accept({ token, password });
              else await auth.reset({ token, newPassword: password });
              setDone(true);
              setToken("");
              setPassword("");
              setConfirmation("");
              tokenByPath.delete(window.location.pathname);
            } catch (failure) {
              setError(failure.message);
            } finally {
              setPending(false);
            }
          }}
        >
          <p>Use de 15 a 128 caracteres. Espaços são permitidos.</p>
          <label>
            Nova senha
            <input
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>
          <label>
            Confirmar senha
            <input
              type="password"
              autoComplete="new-password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              required
            />
          </label>
          <ErrorAlert message={error} />
          <button className="full" disabled={pending}>
            {pending ? "Salvando…" : "Definir senha"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
