import { api } from "./api";

export const appearance = {
  find: (options) => api("/company/appearance", options),
  palette: (palette, version) =>
    api("/company/appearance", { method: "PATCH", body: { palette, version } }),
  logo: (file, version) => {
    const body = new FormData();
    body.append("file", file);
    body.append("version", String(version));
    return api("/company/appearance/logo", { method: "PUT", body });
  },
  removeLogo: (version) => api(`/company/appearance/logo?version=${version}`, { method: "DELETE" }),
  preference: (options) => api("/preferences/current", options),
  theme: (theme) => api("/preferences/current", { method: "PATCH", body: { theme } }),
};
