import { api, clearCsrf, loadCsrf } from "./api";

async function authenticated(path, body) {
  const session = await api(path, { method: "POST", body, publicRequest: true });
  clearCsrf();
  await loadCsrf();
  return session;
}

export const auth = {
  session: () => api("/auth/session", { publicRequest: true }),
  login: async (body) => {
    await loadCsrf();
    return authenticated("/auth/login", body);
  },
  replace: (body) => authenticated("/auth/sessionReplacement", body),
  activity: () => api("/auth/activity", { method: "POST" }),
  logout: async () => {
    await api("/auth/logout", { method: "POST", publicRequest: true });
    clearCsrf();
  },
  recover: (email) =>
    api("/auth/passwordRecovery", {
      method: "POST",
      body: { email },
      publicRequest: true,
    }),
  reset: (body) => api("/auth/passwordReset", { method: "POST", body, publicRequest: true }),
  accept: (body) =>
    api("/auth/invitationAcceptance", { method: "POST", body, publicRequest: true }),
  password: (body) => api("/auth/password", { method: "PATCH", body }),
};
