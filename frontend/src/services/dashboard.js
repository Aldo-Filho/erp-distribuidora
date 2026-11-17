import { api } from "./api";
const query = filter => new URLSearchParams(Object.entries(filter).filter(([,v]) => v !== undefined && v !== null && v !== "")).toString();
export const dashboard = {
  current: (filter, options) => api(`/dashboard?${query(filter)}`, options),
  details: (id, filter, page, options) => api(`/dashboard/widgets/${encodeURIComponent(id)}/details?${query({...filter, page, size:20})}`, options),
  preference: options => api("/preferences/current/dashboard", options),
  save: body => api("/preferences/current/dashboard", { method:"PUT", body }),
  reset: revision => api(`/preferences/current/dashboard?revision=${revision}`, { method:"DELETE" }),
};
