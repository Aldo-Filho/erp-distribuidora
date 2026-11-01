import { Navigate, Outlet, useLocation } from "react-router-dom";
import useAuth from "../auth/useAuth";
import ErrorAlert from "../components/feedback/ErrorAlert";

export default function ProtectedRoute({ permission, anyPermissions }) {
  const { session, loading, error, refresh, has } = useAuth();
  const location = useLocation();
  if (loading)
    return (
      <div className="screen-center" role="status">
        Carregando acesso…
      </div>
    );
  if (!session && error)
    return (
      <div className="screen-center">
        <ErrorAlert message={error} retry={refresh} />
      </div>
    );
  if (!session)
    return (
      <Navigate
        to="/login"
        state={{ returnTo: location.pathname + location.search, returnContext: location.state }}
        replace
      />
    );
  if ((permission && !has(permission)) || (anyPermissions && !anyPermissions.some(has)))
    return (
      <section className="panel">
        <h1>Acesso negado</h1>
        <p>Seu cargo não permite abrir esta tela.</p>
      </section>
    );
  return <Outlet />;
}
