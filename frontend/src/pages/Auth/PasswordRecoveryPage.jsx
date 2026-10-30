import { useState } from "react";
import { auth } from "../../services/auth";
import AuthLayout from "./AuthLayout";
import ErrorAlert from "../../components/feedback/ErrorAlert";

export default function PasswordRecoveryPage() {
  const [email, setEmail] = useState("");
  const [pending, setPending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  return (
    <AuthLayout title="Recuperar acesso" showLogo={false}>
      {sent ? (
        <p className="success" role="status">
          Se houver uma conta ativa para este e-mail, enviaremos as instruções. Confira também o
          spam.
        </p>
      ) : (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            setError("");
            setPending(true);
            try {
              await auth.recover(email);
              setSent(true);
            } catch (failure) {
              setError(failure.message);
            } finally {
              setPending(false);
            }
          }}
        >
          <label>
            E-mail
            <input
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <ErrorAlert message={error} />
          <button className="full" disabled={pending}>
            {pending ? "Solicitando…" : "Solicitar recuperação"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
