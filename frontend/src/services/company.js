import { api } from "./api";

export const company = {
  find: (options) => api("/company/profile", options),
  save: (body) => api("/company/profile", { method: "PUT", body }),
};
