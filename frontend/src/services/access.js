import { api } from "./api";

export const access = {
  users: () => api("/user/search"),
  updateUser: (id, body) => api("/user/" + id, { method: "PATCH", body }),
  deleteUser: (id) => api("/user/" + id, { method: "DELETE" }),
  recoverUser: (id) => api("/user/" + id + "/passwordRecovery", { method: "POST" }),
  roles: () => api("/role/search"),
  createRole: (body) => api("/role", { method: "POST", body }),
  updateRole: (id, body) => api("/role/" + id, { method: "PATCH", body }),
  deleteRole: (id) => api("/role/" + id, { method: "DELETE" }),
  permissions: () => api("/permission/search"),
  grants: (id) => api("/role/" + id + "/permissions"),
  grant: (id, permissionId) =>
    api("/role/" + id + "/permissions", { method: "POST", body: { permissionId } }),
  revoke: (id, permissionId) =>
    api("/role/" + id + "/permissions/" + permissionId, { method: "DELETE" }),
  invitations: (page = 0, email = "") =>
    api("/user/invitation/search?" + new URLSearchParams({ page, email })),
  invite: (body) => api("/user/invitation", { method: "POST", body }),
  resend: (id) => api("/user/invitation/" + id + "/resend", { method: "POST" }),
  cancelInvite: (id) => api("/user/invitation/" + id, { method: "DELETE" }),
};
