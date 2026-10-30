export const navigation = [
  { path: "/products", label: "Produtos", anyPermissions: ["products.read", "stock.read"], icon: "products" },
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
  const entry = navigation.find((item) => item.path === destination);
  return entry && canAccessEntry(entry, session)
    ? destination
    : firstRoute(session);
}
