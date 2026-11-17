import { NavLink, Outlet, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Wallet,
  Palette,
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
import useAppearance from "../../appearance/useAppearance";
import useAuth from "../../auth/useAuth";
import { canAccessEntry, navigation, navigationGroups } from "../../routes/navigation";
import ErrorAlert from "../feedback/ErrorAlert";

const icons = {
  dashboard: LayoutDashboard,
  finance: Wallet,
  appearance: Palette,
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
  const { settings } = useAppearance();
  const [failedLogo, setFailedLogo] = useState(null);
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [failure, setFailure] = useState("");
  const visibleGroups = navigationGroups
    .map((group) => ({
      ...group,
      entries: navigation.filter(
        (entry) => entry.group === group.id && canAccessEntry(entry, session),
      ),
    }))
    .filter((group) => group.entries.length > 0);
  return (
    <div className={"app-shell" + (collapsed ? " is-collapsed" : "")}>
      <aside id="main-sidebar" className="sidebar">
        <div className="brand" aria-label="JamsHub — Gestão para distribuidoras">
          <span className="brand-mark">
            {settings.logoUrl && failedLogo !== settings.logoUrl ? (
              <img
                className="company-brand-logo"
                src={`/api${settings.logoUrl}`}
                alt="Logo da empresa"
                onError={() => setFailedLogo(settings.logoUrl)}
              />
            ) : (
              <Package size={20} aria-hidden="true" />
            )}
          </span>
          <span className="sidebar-label">JamsHub</span>
        </div>
        <nav aria-label="Navegação principal">
          {visibleGroups.map((group) => (
            <section
              className="sidebar-group"
              key={group.id}
              aria-labelledby={`sidebar-group-${group.id}`}
            >
              <h2 className="sidebar-group-title" id={`sidebar-group-${group.id}`}>
                {group.label}
              </h2>
              <div className="sidebar-group-links">
                {group.entries.map((entry) => {
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
              </div>
            </section>
          ))}
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
