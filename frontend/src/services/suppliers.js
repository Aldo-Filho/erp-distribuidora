import { api } from "./api";

export function queryString(filter = {}) {
  return new URLSearchParams(
    Object.entries(filter).filter(
      ([, value]) => value !== undefined && value !== null && String(value).trim() !== "",
    ),
  ).toString();
}

export const suppliers = {
  search: (filter, page = 0, size = 25, options) =>
    api("/supplier/search?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api("/supplier/" + id, options),
  register: (body) => api("/supplier", { method: "POST", body }),
  update: (id, body) => api("/supplier/" + id, { method: "PATCH", body }),
  addresses: (supplierId, options) =>
    api("/supplierAddress/search?" + queryString({ supplierId }), options),
  registerAddress: (body) => api("/supplierAddress", { method: "POST", body }),
  updateAddress: (id, body) => api("/supplierAddress/" + id, { method: "PATCH", body }),
  deleteAddress: (id) => api("/supplierAddress/" + id, { method: "DELETE" }),
};
