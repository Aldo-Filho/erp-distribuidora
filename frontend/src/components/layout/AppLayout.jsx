import { NavLink, Outlet } from "react-router-dom";
import {
  Users,
  ShieldCheck,
  Mail,
  Monitor,
  UserRound,
  ScrollText,
  KeyRound,
  LogOut,
  Menu,
  X,
} from "lucide-react";
import { useState } from "react";
import useAuth from "../../auth/useAuth";
import { navigation } from "../../routes/navigation";
import ErrorAlert from "../feedback/ErrorAlert";

const icons = {
  users: Users,
  roles: KeyRound,
  mail: Mail,
  shield: ShieldCheck,
  audit: ScrollText,
  monitor: Monitor,
  account: UserRound,
};

export default function AppLayout() {
  const { session, has, logout, error } = useAuth();
  const [open, setOpen] = useState(false);
  const [failure, setFailure] = useState("");
  return (
    <div className="app-shell">
      <aside className={"sidebar " + (open ? "is-open" : "")}>
        <div className="brand">
          <span className="brand-mark">ED</span>
          <span>
            ERP<small>Distribuidora</small>
          </span>
          <button
            className="icon-button mobile-only"
            aria-label="Fechar menu"
            onClick={() => setOpen(false)}
          >
            <X size={20} />
          </button>
        </div>
        <nav aria-label="Navegação principal">
          {navigation
            .filter((entry) => !entry.permission || has(entry.permission))
            .map((entry) => {
              const Icon = icons[entry.icon];
              return (
                <NavLink to={entry.path} key={entry.path} onClick={() => setOpen(false)}>
                  <Icon size={18} />
                  {entry.label}
                </NavLink>
              );
            })}
        </nav>
        <div className="sidebar-footer">
          <strong>{session.name}</strong>
          <small>{session.roleName}</small>
          <button
            className="logout-button"
            onClick={async () => {
              try {
                await logout();
              } catch (issue) {
                setFailure(issue.message);
              }
            }}
          >
            <LogOut size={16} /> Sair
          </button>
        </div>
      </aside>
      {open && (
        <button className="nav-overlay" aria-label="Fechar menu" onClick={() => setOpen(false)} />
      )}
      <div className="workspace">
        <header className="topbar">
          <button
            className="icon-button mobile-only"
            aria-label="Abrir menu"
            onClick={() => setOpen(true)}
          >
            <Menu size={22} />
          </button>
          <span>Administração de acessos</span>
          <span className="environment">Local</span>
        </header>
        <main>
          <ErrorAlert message={failure || error} />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
