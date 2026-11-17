import { NavLink, Outlet, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Wallet,
  Palette,
  Building2,
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
  Settings,
  ChevronDown,
  ArrowDownLeft,
  ArrowUpRight,
  ChartNoAxesCombined,
  Tags,
  Menu,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import useAppearance from "../../appearance/useAppearance";
import useAuth from "../../auth/useAuth";
import { canAccessEntry, navigation, navigationGroups } from "../../routes/navigation";
import ErrorAlert from "../feedback/ErrorAlert";

const icons = {
  dashboard: LayoutDashboard,
  finance: Wallet,
  receivables: ArrowDownLeft,
  payables: ArrowUpRight,
  cashFlow: ChartNoAxesCombined,
  categories: Tags,
  appearance: Palette,
  company: Building2,
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

function SidebarLinks({ entries, pathname, onNavigate }) {
  return entries.map((entry) => {
    const Icon = icons[entry.icon];
    return (
      <NavLink
        to={entry.path}
        key={entry.path}
        className={({ isActive }) =>
          isActive || (entry.path === "/products" && pathname === "/stock/history")
            ? "active" : undefined
        }
        aria-label={entry.label}
        title={entry.label}
        onClick={onNavigate}
      >
        <Icon size={18} aria-hidden="true" />
        <span className="sidebar-label">{entry.label}</span>
      </NavLink>
    );
  });
}

function SidebarDisclosure({ id, label, active, icon: Icon, onExpand, collapsed, children }) {
  const [open, setOpen] = useState(active);
  return (
    <section className="sidebar-disclosure">
      <button
        className={"sidebar-disclosure-toggle" + (active ? " has-active-page" : "")}
        aria-expanded={open && !collapsed}
        aria-controls={id}
        title={label}
        onClick={() => {
          onExpand?.();
          setOpen((current) => collapsed ? true : !current);
        }}
      >
        {Icon && <Icon size={18} aria-hidden="true" />}
        <span className="sidebar-label">{label}</span>
        <ChevronDown className="sidebar-disclosure-chevron" size={14} aria-hidden="true" />
      </button>
      <div id={id} className="sidebar-disclosure-content" hidden={!open}>
        {children}
      </div>
    </section>
  );
}

export default function AppLayout() {
  const { session, logout, error } = useAuth();
  const { settings } = useAppearance();
  const [failedLogo, setFailedLogo] = useState(null);
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  const [failure, setFailure] = useState("");
  const [mobileOpen, setMobileOpen] = useState(false);
  const menuButton = useRef(null);
  const sidebar = useRef(null);
  function closeMobileMenu() {
    if (!mobileOpen) return;
    setMobileOpen(false);
    menuButton.current?.focus();
  }
  useEffect(() => {
    if (!mobileOpen) return;
    sidebar.current?.querySelector("button, a")?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function handleKey(event) {
      if (event.key === "Escape") {
        event.preventDefault();
        setMobileOpen(false);
        menuButton.current?.focus();
      }
      if (event.key === "Tab") {
        const controls = [...sidebar.current.querySelectorAll("a, button")]
          .filter((element) => element.getClientRects().length > 0);
        const first = controls[0];
        const last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first?.focus();
        }
      }
    }
    const desktop = window.matchMedia("(min-width: 781px)");
    function handleDesktop(event) {
      if (event.matches) setMobileOpen(false);
    }
    desktop.addEventListener("change", handleDesktop);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      desktop.removeEventListener("change", handleDesktop);
      document.removeEventListener("keydown", handleKey);
    };
  }, [mobileOpen]);
  const visibleGroups = navigationGroups
    .map((group) => ({
      ...group,
      entries: navigation.filter(
        (entry) => entry.group === group.id && canAccessEntry(entry, session),
      ),
    }))
    .filter((group) => group.entries.length > 0);
  const settingsGroups = visibleGroups.filter((group) => group.parent === "settings");
  const isGroupActive = (group) => group.entries.some((entry) =>
    location.pathname === entry.path || location.pathname.startsWith(`${entry.path}/`));
  const settingsActive = settingsGroups.some(isGroupActive);
  return (
    <div className={"app-shell" + (collapsed ? " is-collapsed" : "") + (mobileOpen ? " is-menu-open" : "")}>
      <button ref={menuButton} className="mobile-menu-button" aria-expanded={mobileOpen}
        aria-controls="main-sidebar" onClick={() => { setCollapsed(false); setMobileOpen(true); }}>
        <Menu size={18} aria-hidden="true" /> Menu
      </button>
      {mobileOpen && <div className="sidebar-backdrop" onClick={closeMobileMenu} aria-hidden="true" />}
      <aside ref={sidebar} id="main-sidebar" className="sidebar">
        <button className="mobile-menu-close" onClick={closeMobileMenu} aria-label="Fechar menu">
          <X size={20} aria-hidden="true" />
        </button>
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
          {visibleGroups.filter((group) => !group.parent).map((group) => (
            <section
              className="sidebar-group"
              key={group.id}
              aria-labelledby={`sidebar-group-${group.id}`}
            >
              <h2 className={"sidebar-group-title" + (group.id === "overview" ? " sr-only" : "")} id={`sidebar-group-${group.id}`}>
                {group.label}
              </h2>
              <div className="sidebar-group-links">
                <SidebarLinks entries={group.entries} pathname={location.pathname} onNavigate={closeMobileMenu} />
              </div>
            </section>
          ))}
          {settingsGroups.length > 0 && (
            <SidebarDisclosure key={`settings:${location.pathname}`} id="sidebar-settings" label="Configurações"
              active={settingsActive} icon={Settings} collapsed={collapsed} onExpand={() => setCollapsed(false)}>
              {settingsGroups.map((group) => (
                <SidebarDisclosure key={`${group.id}:${location.pathname}`} id={`sidebar-settings-${group.id}`}
                  label={group.label} active={isGroupActive(group)}>
                  <SidebarLinks entries={group.entries} pathname={location.pathname} onNavigate={closeMobileMenu} />
                </SidebarDisclosure>
              ))}
            </SidebarDisclosure>
          )}
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
          <NavLink to="/account" className="sidebar-account" title="Minha conta" aria-label="Minha conta" onClick={closeMobileMenu}>
            <UserRound size={18} aria-hidden="true" />
            <div className="sidebar-user">
              <strong>{session.name}</strong>
              <small>Minha conta</small>
            </div>
          </NavLink>
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
      <div className="workspace" inert={mobileOpen}>
        <main>
          <ErrorAlert message={failure || error} />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
