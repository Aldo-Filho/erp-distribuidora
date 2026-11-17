import { api, allPages } from "./api";

export function queryString(filter = {}) {
  return new URLSearchParams(
    Object.entries(filter).filter(
      ([, value]) => value !== undefined && value !== null && String(value).trim() !== "",
    ),
  ).toString();
}

export const suppliers = {
  search: (filter, page = 0, size = 25, options) =>
    api("/suppliers?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api("/suppliers/" + id, options),
  register: (body) => api("/suppliers", { method: "POST", body }),
  update: (id, body) => api("/suppliers/" + id, { method: "PATCH", body }),
  addresses: (supplierId, options) =>
    allPages("/supplier-addresses?" + queryString({ supplierId }), options),
  registerAddress: (body) => api("/supplier-addresses", { method: "POST", body }),
  updateAddress: (id, body) => api("/supplier-addresses/" + id, { method: "PATCH", body }),
  deleteAddress: (id) => api("/supplier-addresses/" + id, { method: "DELETE" }),
};
