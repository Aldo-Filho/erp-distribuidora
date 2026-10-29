import { api } from "./api";

export function queryString(filter = {}) {
  return new URLSearchParams(
    Object.entries(filter).filter(
      ([, value]) => value !== undefined && value !== null && String(value).trim() !== "",
    ),
  ).toString();
}

export const customers = {
  search: (filter, page = 0, size = 25, options) =>
    api("/customer/search?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api("/customer/" + id, options),
  register: (body) => api("/customer", { method: "POST", body }),
  update: (id, body) => api("/customer/" + id, { method: "PATCH", body }),
  addresses: (customerId, options) =>
    api("/customerAddress/search?" + queryString({ customerId }), options),
  registerAddress: (body) => api("/customerAddress", { method: "POST", body }),
  updateAddress: (id, body) => api("/customerAddress/" + id, { method: "PATCH", body }),
  deleteAddress: (id) => api("/customerAddress/" + id, { method: "DELETE" }),
};
