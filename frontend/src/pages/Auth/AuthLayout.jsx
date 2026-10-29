import { Link } from "react-router-dom";

export default function AuthLayout({ title, children }) {
  return (
    <div className="auth-screen">
      <div className="auth-brand">
        <span className="brand-mark">ED</span>
        <span>ERP Distribuidora</span>
      </div>
      <section className="auth-card">
        <h1>{title}</h1>
        {children}
      </section>
      <Link className="auth-footer" to="/login">
        Voltar ao acesso
      </Link>
    </div>
  );
}
