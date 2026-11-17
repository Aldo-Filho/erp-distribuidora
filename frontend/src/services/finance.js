import { api, allPages } from "./api";
import { queryString } from "./suppliers";
const collection = (path) => ({
  search: (filters = {}, options) => api(`${path}?${queryString(filters)}`, options),
  findOne: (id, options) => api(`${path}/${id}`, options),
  register: (body) => api(path, { method: "POST", body }),
  update: (id, body) => api(`${path}/${id}`, { method: "PATCH", body }),
  options: (filters = {}, options) => allPages(`${path}?${queryString(filters)}`, options),
});
const titles = (path) => ({
  search: (filters = {}, options) => api(`${path}?${queryString(filters)}`, options),
  summary: (filters = {}, options) => api(`${path}/summary?${queryString(filters)}`, options),
  findOne: (id, options) => api(`${path}/${id}`, options),
  history: (id, page = 0, options) => api(`${path}/${id}/settlements?page=${page}&size=25`, options),
  reversals: (id, paymentId, options) => api(`${path}/${id}/settlements/${paymentId}/reversals`, options),
  settle: (id, body) => api(`${path}/${id}/settlements`, { method: "POST", body }),
  reverse: (id, paymentId, body) => api(`${path}/${id}/settlements/${paymentId}/reversals`, { method: "POST", body }),
});
export const receivables = titles("/receivables");
export const payables = titles("/payables");
export const financialCategories = collection("/financial-categories");
export const bankAccounts = collection("/bank-accounts");
export const cashFlow = {
  search: (filters, options) => api(`/cash-flow?${queryString(filters)}`, options),
  summary: (filters, options) => api(`/cash-flow/summary?${queryString(filters)}`, options),
  balance: (filters, options) => api(`/cash-flow/balance?${queryString(filters)}`, options),
};
