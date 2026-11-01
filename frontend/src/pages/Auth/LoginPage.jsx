import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import useAuth from "../../auth/useAuth";
import { auth } from "../../services/auth";
import { safeReturn } from "../../routes/navigation";
import AuthLayout from "./AuthLayout";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import SessionReplacementDialog from "../../components/feedback/SessionReplacementDialog";

export default function LoginPage() {
  const { session, loading, expired, signedIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [challenge, setChallenge] = useState(null);
  const success = (value) => {
    signedIn(value);
    setPassword("");
    setChallenge(null);
    navigate(safeReturn(location.state?.returnTo, value), {
      replace: true,
      state: location.state?.returnContext,
    });
  };
  if (loading) return <div className="screen-center">Carregando acesso…</div>;
  if (session)
    return (
      <Navigate
        to={safeReturn(location.state?.returnTo, session)}
        state={location.state?.returnContext}
        replace
      />
    );
  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setPending(true);
    try {
      success(await auth.login({ email, password }));
    } catch (failure) {
      if (failure.status === 409 && failure.data?.code === "SESSION_LIMIT_REACHED") {
        setChallenge(failure.data);
        setPassword("");
      } else setError(failure.message);
    } finally {
      setPending(false);
    }
  };
  const replace = async (sessionId) => {
    try {
      success(await auth.replace({ challengeToken: challenge.challengeToken, sessionId }));
    } catch (failure) {
      if (failure.status === 409 && failure.data?.code === "SESSION_LIMIT_REACHED") {
        setChallenge(failure.data);
        return;
      }
      throw failure;
    }
  };
  return (
    <AuthLayout title="Entrar" showLogo={false} showReturn={false}>
      {expired && (
        <p className="notice">
          Entre novamente para continuar. Dados não salvos podem precisar ser preenchidos de novo.
        </p>
      )}
      <form onSubmit={submit}>
        <label>
          E-mail
          <input
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>
        <label>
          Senha
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        <ErrorAlert message={error} />
        <button className="full" disabled={pending}>
          {pending ? "Entrando…" : "Entrar"}
        </button>
      </form>
      <Link className="form-link" to="/forgot-password">
        Esqueci minha senha
      </Link>
      {challenge && (
        <SessionReplacementDialog
          key={challenge.challengeToken}
          challenge={challenge}
          replace={replace}
          cancel={() => setChallenge(null)}
        />
      )}
    </AuthLayout>
  );
}
