import { api, allPages } from "./api";
import { queryString } from "./suppliers";

export const productSuppliers = {
  search: (filter, options) => allPages("/product-suppliers?" + queryString(filter), options),
  register: (body) => api("/product-suppliers", { method: "POST", body }),
  update: (id, body) => api(`/product-suppliers/${id}`, { method: "PATCH", body }),
  options: (productSupplierId, filter, page = 0, size = 25, options) =>
    api(
      "/product-supplier-options?" + queryString({ ...filter, productSupplierId, page, size }),
      options,
    ),
  registerOption: (body) => api("/product-supplier-options", { method: "POST", body }),
  updateOption: (id, body) => api(`/product-supplier-options/${id}`, { method: "PATCH", body }),
};
