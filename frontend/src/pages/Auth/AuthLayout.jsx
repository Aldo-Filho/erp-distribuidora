import { Link } from "react-router-dom";

export default function AuthLayout({ title, children, showLogo = true, showReturn = true }) {
  return (
    <div className="auth-screen">
      <div className="auth-brand">
        {showLogo && (
          <span className="brand-mark">
            <img src="/jamshub-logo.png" alt="" />
          </span>
        )}
        <span>JamsHub</span>
      </div>
      <section className="auth-card">
        <h1>{title}</h1>
        {children}
      </section>
      {showReturn && (
        <Link className="auth-footer" to="/login">
          Voltar ao acesso
        </Link>
      )}
    </div>
  );
}
