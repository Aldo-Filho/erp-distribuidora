import { api } from "./api";

export const security = {
  sessions: (userId, page = 0) =>
    api(userId ? "/user/" + userId + "/sessions?page=" + page : "/auth/sessions"),
  revoke: (id, userId) =>
    api(userId ? "/user/" + userId + "/sessions/" + id : "/auth/sessions/" + id, {
      method: "DELETE",
    }),
  settings: () => api("/security/settings"),
  updateSettings: (body) => api("/security/settings", { method: "PATCH", body }),
  audit: (filters) =>
    api(
      "/security/audit/search?" +
        new URLSearchParams(
          Object.entries(filters).filter(([, value]) => value !== "" && value != null),
        ),
    ),
};
