import { api, allPages } from "./api";

export const access = {
  user: (id, options) => api("/users/" + id, options),
  users: () => allPages("/users"),
  updateUser: (id, body) => api("/users/" + id, { method: "PATCH", body }),
  deleteUser: (id) => api("/users/" + id, { method: "DELETE" }),
  recoverUser: (id) => api("/users/" + id + "/password-recoveries", { method: "POST" }),
  roles: () => allPages("/roles"),
  createRole: (body) => api("/roles", { method: "POST", body }),
  updateRole: (id, body) => api("/roles/" + id, { method: "PATCH", body }),
  deleteRole: (id) => api("/roles/" + id, { method: "DELETE" }),
  permissions: () => allPages("/permissions"),
  grants: (id) => allPages("/roles/" + id + "/permissions"),
  grant: (id, permissionId) =>
    api("/roles/" + id + "/permissions", { method: "POST", body: { permissionId } }),
  revoke: (id, permissionId) =>
    api("/roles/" + id + "/permissions/" + permissionId, { method: "DELETE" }),
  invitations: (page = 0, email = "") =>
    api("/user-invitations?" + new URLSearchParams({ page, email })),
  invite: (body) => api("/user-invitations", { method: "POST", body }),
  resend: (id) => api("/user-invitations/" + id + "/deliveries", { method: "POST" }),
  cancelInvite: (id) => api("/user-invitations/" + id, { method: "DELETE" }),
};
