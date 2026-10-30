import { api } from "./api";
import { queryString } from "./suppliers";

export const productSuppliers = {
  search: (filter, options) => api("/productSupplier/search?" + queryString(filter), options),
  register: (body) => api("/productSupplier", { method: "POST", body }),
  update: (id, body) => api(`/productSupplier/${id}`, { method: "PATCH", body }),
  options: (productSupplierId, filter, page = 0, size = 25, options) =>
    api(
      "/productSupplierOption/search?" + queryString({ ...filter, productSupplierId, page, size }),
      options,
    ),
  registerOption: (body) => api("/productSupplierOption", { method: "POST", body }),
  updateOption: (id, body) => api(`/productSupplierOption/${id}`, { method: "PATCH", body }),
};
