import { parseHistoryQuery } from "./stockHistoryQuery";

export const navigation = [
  {
    path: "/products",
    label: "Produtos",
    anyPermissions: ["products.read", "stock.read"],
    icon: "products",
  },
  { path: "/warehouses", label: "Depósitos", permission: "warehouses.read", icon: "warehouses" },
  { path: "/customers", label: "Clientes", permission: "customers.read", icon: "customers" },
  { path: "/suppliers", label: "Fornecedores", permission: "suppliers.read", icon: "suppliers" },
  { path: "/purchases", label: "Compras", permission: "purchases.read", icon: "purchases" },
  { path: "/orders", label: "Vendas", permission: "orders.read", icon: "orders" },
  { path: "/access/roles", label: "Cargos", permission: "access.roles.manage", icon: "roles" },
  {
    path: "/access/users",
    label: "Funcionários",
    permission: "access.users.manage",
    icon: "users",
  },
  {
    path: "/access/invitations",
    label: "Convites",
    permission: "access.users.manage",
    icon: "mail",
  },
  {
    path: "/security/settings",
    label: "Política de acesso",
    permission: "security.settings.manage",
    icon: "shield",
  },
  { path: "/security/sessions", label: "Sessões", icon: "monitor" },
  { path: "/security/audit", label: "Auditoria", permission: "security.audit.read", icon: "audit" },
  { path: "/account", label: "Minha conta", icon: "account" },
];

export function canAccessEntry(entry, session) {
  if (entry.anyPermissions) {
    return entry.anyPermissions.some((permission) => session.permissionKeys.includes(permission));
  }
  return !entry.permission || session.permissionKeys.includes(entry.permission);
}

export function firstRoute(session) {
  return (
    navigation.find(
      (entry) => (entry.permission || entry.anyPermissions) && canAccessEntry(entry, session),
    )?.path || "/account"
  );
}

export function safeReturn(path, session) {
  const destination = path === "/stock" ? "/products" : path;
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
    if (
      (suffix?.startsWith("/variants/") && module !== "products") ||
      (module === "orders" && operation !== "new" && suffix !== "/edit")
    )
      return firstRoute(session);
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
  const entry = navigation.find((item) => item.path === destination);
  return !query && entry && canAccessEntry(entry, session) ? destination : firstRoute(session);
}

export function validId(value) {
  return /^[1-9]\d*$/.test(String(value || "")) && Number.isSafeInteger(Number(value));
}
