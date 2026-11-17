import { api, allPages } from "./api";

export function queryString(filter = {}) {
  return new URLSearchParams(
    Object.entries(filter).filter(
      ([, value]) => value !== undefined && value !== null && String(value).trim() !== "",
    ),
  ).toString();
}

export const customers = {
  search: (filter, page = 0, size = 25, options) =>
    api("/customers?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api("/customers/" + id, options),
  register: (body) => api("/customers", { method: "POST", body }),
  update: (id, body) => api("/customers/" + id, { method: "PATCH", body }),
  addresses: (customerId, options) =>
    allPages("/customer-addresses?" + queryString({ customerId }), options),
  registerAddress: (body) => api("/customer-addresses", { method: "POST", body }),
  updateAddress: (id, body) => api("/customer-addresses/" + id, { method: "PATCH", body }),
  deleteAddress: (id) => api("/customer-addresses/" + id, { method: "DELETE" }),
};
