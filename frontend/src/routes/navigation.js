export const navigation = [
  {
    path: "/access/users",
    label: "Funcionários",
    permission: "access.users.manage",
    icon: "users",
  },
  { path: "/access/roles", label: "Cargos", permission: "access.roles.manage", icon: "roles" },
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
  { path: "/security/audit", label: "Auditoria", permission: "security.audit.read", icon: "audit" },
  { path: "/security/sessions", label: "Sessões", icon: "monitor" },
  { path: "/account", label: "Minha conta", icon: "account" },
];

export function firstRoute(session) {
  return (
    navigation.find(
      (entry) => entry.permission && session.permissionKeys.includes(entry.permission),
    )?.path || "/account"
  );
}

export function safeReturn(path, session) {
  const entry = navigation.find((item) => item.path === path);
  return entry && (!entry.permission || session.permissionKeys.includes(entry.permission))
    ? path
    : firstRoute(session);
}
