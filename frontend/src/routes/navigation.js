import { parseHistoryQuery } from "./stockHistoryQuery.js";

// Financial routes share the same permission contract as their protected pages.
export const financialRoutePermissions = {
  "/finance/receivables": "finance.read",
  "/finance/payables": "finance.read",
  "/finance/cash-flow": "finance.read",
  "/finance/categories": "finance.configure",
  "/finance/bank-accounts": "finance.configure",
};

export const navigationGroups = [
  { id: "overview", label: "Início" },
  { id: "operations", label: "Operações" },
  { id: "catalogs", label: "Cadastros" },
  { id: "finance", label: "Financeiro" },
  { id: "settings", label: "Empresa e aparência", parent: "settings" },
  { id: "finance-settings", label: "Cadastros financeiros", parent: "settings" },
  { id: "team", label: "Equipe", parent: "settings" },
  { id: "security", label: "Segurança", parent: "settings" },
];

export const navigation = [
  { path: "/dashboard", group: "overview", label: "Início", icon: "dashboard" },
  {
    path: "/products", group: "catalogs",
    label: "Produtos",
    anyPermissions: ["products.read", "stock.read"],
    icon: "products",
  },
  { path: "/warehouses", group: "catalogs", label: "Depósitos", permission: "warehouses.read", icon: "warehouses" },
  { path: "/customers", group: "catalogs", label: "Clientes", permission: "customers.read", icon: "customers" },
  { path: "/suppliers", group: "catalogs", label: "Fornecedores", permission: "suppliers.read", icon: "suppliers" },
  { path: "/orders", group: "operations", label: "Vendas", permission: "orders.read", icon: "orders" },
  { path: "/purchases", group: "operations", label: "Compras", permission: "purchases.read", icon: "purchases" },
  { path: "/purchase-suggestions", group: "operations", label: "Sugestões de compra", permission: "purchases.read", icon: "purchases" },
  ...Object.entries(financialRoutePermissions).map(([path, permission]) => ({
    path, permission, group: permission === "finance.configure" ? "finance-settings" : "finance", icon: ({
      "/finance/receivables": "receivables", "/finance/payables": "payables",
      "/finance/cash-flow": "cashFlow", "/finance/categories": "categories",
      "/finance/bank-accounts": "finance",
    })[path], label: ({
      "/finance/receivables": "Contas a receber", "/finance/payables": "Contas a pagar",
      "/finance/cash-flow": "Fluxo de caixa", "/finance/categories": "Categorias financeiras",
      "/finance/bank-accounts": "Contas bancárias e caixa",
    })[path],
  })),
  { path: "/access/roles", group: "team", label: "Cargos", permission: "access.roles.manage", icon: "roles" },
  {
    path: "/access/users", group: "team",
    label: "Funcionários",
    permission: "access.users.manage",
    icon: "users",
  },
  {
    path: "/access/invitations", group: "team",
    label: "Convites",
    permission: "access.users.manage",
    icon: "mail",
  },
  {
    path: "/security/settings", group: "security",
    label: "Política de acesso",
    permission: "security.settings.manage",
    icon: "shield",
  },
  { path: "/security/sessions", group: "security", label: "Sessões", icon: "monitor" },
  { path: "/security/audit", group: "security", label: "Auditoria", permission: "security.audit.read", icon: "audit" },
  {
    path: "/settings/appearance", group: "settings",
    label: "Aparência",
    permission: "company.appearance.manage",
    icon: "appearance",
  },
  {
    path: "/settings/company", group: "settings", label: "Dados da empresa",
    permission: "company.read", icon: "company",
  },
  { path: "/account", group: "account", label: "Minha conta", icon: "account" },
];

export function canAccessEntry(entry, session) {
  if (entry.anyPermissions) {
    return entry.anyPermissions.some((permission) => session.permissionKeys.includes(permission));
  }
  return !entry.permission || session.permissionKeys.includes(entry.permission);
}

export function firstRoute() {
  return "/dashboard";
}

export function safeReturn(path, session) {
  const destination = path === "/" ? "/dashboard" : path;
  if (
    typeof destination !== "string" ||
    !destination.startsWith("/") ||
    destination.startsWith("//") ||
    /[\\#]/.test(destination)
  )
    return firstRoute(session);
  const [pathname, query = "", extra] = destination.split("?");
  if (extra !== undefined) return firstRoute(session);
  const params = new URLSearchParams(query);
  if (pathname === "/stock/history") {
    return !parseHistoryQuery(params).error && session.permissionKeys.includes("stock.read")
      ? destination
      : firstRoute(session);
  }
  const match =
    /^\/(purchases|products|suppliers|customers|orders)\/(new|([1-9]\d*)(\/edit|\/variants\/([1-9]\d*))?)$/.exec(
      pathname,
    );
  if (match && (!match[3] || validId(match[3])) && (!match[5] || validId(match[5]))) {
    const [, module, operation, , suffix] = match;
    if (suffix?.startsWith("/variants/") && module !== "products") return firstRoute(session);
    const sections = {
      products: ["general", "commercial", "variants", "presentations", "suppliers", "stock"],
      suppliers: ["general", "addresses", "links", "metadata"],
      customers: ["general", "addresses", "prices", "metadata"],
    };
    const validQuery =
      !query ||
      (operation !== "new" &&
        suffix !== "/edit" &&
        params.size === 1 &&
        sections[module]?.includes(params.get("section")));
    return validQuery && session.permissionKeys.includes(`${module}.read`)
      ? destination
      : firstRoute(session);
  }
  if (/^\/finance\/(receivables|payables)$/.test(pathname) && params.size === 1 &&
      validId(params.get("documentId")) && session.permissionKeys.includes("finance.read")) return destination;
  if (dashboardDrillDown(pathname, params, session)) return destination;
  const entry = navigation.find((item) => item.path === destination);
  return !query && entry && canAccessEntry(entry, session) ? destination : firstRoute(session);
}

export function validId(value) {
  return /^[1-9]\d*$/.test(String(value || "")) && Number.isSafeInteger(Number(value));
}

// Only the explicit dashboard filters may survive a login return. No arbitrary query passthrough.
function dashboardDrillDown(pathname, params, session) {
  const keys = [...params.keys()];
  if (new Set(keys).size !== keys.length) return false;
  const has = permission => session.permissionKeys.includes(permission);
  const day = value => /^\d{4}-\d{2}-\d{2}$/.test(value || "") &&
    !Number.isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0,10) === value;
  if (pathname === "/orders" && has("orders.read")) {
    return keys.length > 0 && keys.every(key => ["status","from","to"].includes(key)) &&
      ["PENDING","CONFIRMED","INVOICED","CANCELED"].includes(params.get("status")) &&
      (!params.has("from") || day(params.get("from"))) && (!params.has("to") || day(params.get("to"))) &&
      (!params.has("from") || !params.has("to") || params.get("from") <= params.get("to"));
  }
  if (/^\/finance\/(receivables|payables)$/.test(pathname) && has("finance.read"))
    return keys.length === 1 && params.get("status") === "OPEN";
  if (pathname === "/finance/cash-flow" && has("finance.read"))
    return keys.length === 3 && keys.every(key => ["kind","from","to"].includes(key)) &&
      params.get("kind") === "ACTUAL" && day(params.get("from")) && day(params.get("to")) && params.get("from") <= params.get("to");
  if (pathname === "/stock" && has("stock.read"))
    return !keys.length || (keys.length === 1 && params.get("lowStock") === "true");
  return false;
}
