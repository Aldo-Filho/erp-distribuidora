import { api, clearCsrf, loadCsrf } from "./api";

async function authenticated(path, body) {
  const session = await api(path, { method: "POST", body, publicRequest: true });
  clearCsrf();
  await loadCsrf();
  return session;
}

export const auth = {
  session: () => api("/auth/sessions/current", { publicRequest: true }),
  login: async (body) => {
    await loadCsrf();
    return authenticated("/auth/sessions", body);
  },
  replace: (body) => authenticated("/auth/session-replacements", body),
  activity: () => api("/auth/session-activities", { method: "POST" }),
  logout: async () => {
    await api("/auth/sessions/current", { method: "DELETE", publicRequest: true });
    clearCsrf();
  },
  recover: (email) =>
    api("/auth/password-recoveries", {
      method: "POST",
      body: { email },
      publicRequest: true,
    }),
  reset: (body) => api("/auth/password-resets", { method: "POST", body, publicRequest: true }),
  accept: (body) =>
    api("/auth/invitation-acceptances", { method: "POST", body, publicRequest: true }),
  password: (body) => api("/auth/password", { method: "PATCH", body }),
};
