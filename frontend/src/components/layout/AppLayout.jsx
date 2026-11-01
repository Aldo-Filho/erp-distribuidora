import { NavLink, Outlet, useLocation } from "react-router-dom";
import {
  Truck,
  Users,
  ContactRound,
  ShieldCheck,
  Mail,
  Monitor,
  UserRound,
  ScrollText,
  KeyRound,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Package,
  Warehouse,
  ShoppingCart,
  ClipboardList,
} from "lucide-react";
import { useState } from "react";
import useAuth from "../../auth/useAuth";
import { canAccessEntry, navigation } from "../../routes/navigation";
import ErrorAlert from "../feedback/ErrorAlert";

const icons = {
  warehouses: Warehouse,
  orders: ShoppingCart,
  purchases: ClipboardList,
  products: Package,
  suppliers: Truck,
  customers: ContactRound,
  users: Users,
  roles: KeyRound,
  mail: Mail,
  shield: ShieldCheck,
  audit: ScrollText,
  monitor: Monitor,
  account: UserRound,
};

export default function AppLayout() {
  const { session, logout, error } = useAuth();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [failure, setFailure] = useState("");
  return (
    <div className={"app-shell" + (collapsed ? " is-collapsed" : "")}>
      <aside id="main-sidebar" className="sidebar">
        <div className="brand" aria-label="JamsHub — Gestão para distribuidoras">
          <span className="brand-mark">
            <Package size={20} aria-hidden="true" />
          </span>
          <span className="sidebar-label">JamsHub</span>
        </div>
        <nav aria-label="Navegação principal">
          {navigation
            .filter((entry) => canAccessEntry(entry, session))
            .map((entry) => {
              const Icon = icons[entry.icon];
              return (
                <NavLink
                  to={entry.path}
                  key={entry.path}
                  className={({ isActive }) =>
                    isActive ||
                    (entry.path === "/products" && location.pathname === "/stock/history")
                      ? "active"
                      : undefined
                  }
                  aria-label={entry.label}
                  title={entry.label}
                >
                  <Icon size={18} />
                  <span className="sidebar-label">{entry.label}</span>
                </NavLink>
              );
            })}
        </nav>
        <button
          className="sidebar-toggle"
          aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
          title={collapsed ? "Expandir menu" : "Recolher menu"}
          aria-expanded={!collapsed}
          aria-controls="main-sidebar"
          onClick={() => setCollapsed((current) => !current)}
        >
          {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
        <div className="sidebar-footer">
          <div className="sidebar-user">
            <strong>{session.name}</strong>
            <small>{session.roleName}</small>
          </div>
          <button
            className="logout-button"
            aria-label="Sair"
            title="Sair"
            onClick={async () => {
              try {
                await logout();
              } catch (issue) {
                setFailure(issue.message);
              }
            }}
          >
            <LogOut size={18} /> <span className="sidebar-label">Sair</span>
          </button>
        </div>
      </aside>
      <div className="workspace">
        <main>
          <ErrorAlert message={failure || error} />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
