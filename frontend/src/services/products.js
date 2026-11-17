import { api, allPages } from "./api";
import { queryString } from "./suppliers";

export const products = {
  search: (filter, page = 0, size = 25, options) =>
    api("/product-catalogs?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api(`/product-catalogs/${id}`, options),
  register: (body) => api("/product-catalogs", { method: "POST", body }),
  update: (id, body) => api(`/product-catalogs/${id}`, { method: "PATCH", body }),
  items: (id, filter, page = 0, size = 25, options) =>
    api(`/product-catalogs/${id}/products?` + queryString({ ...filter, page, size }), options),
  registerItem: (id, body) => api(`/product-catalogs/${id}/products`, { method: "POST", body }),
  updateItem: (id, body) => api(`/products/${id}`, { method: "PATCH", body }),
  findItem: (id, options) => api(`/products/${id}`, options),
  presentations: (productId, filter, page = 0, size = 25, options) =>
    api("/product-presentations?" + queryString({ ...filter, productId, page, size }), options),
  registerPresentation: (body) => api("/product-presentations", { method: "POST", body }),
  findPresentation: (id, options) => api(`/product-presentations/${id}`, options),
  updatePresentation: (id, body) => api(`/product-presentations/${id}`, { method: "PATCH", body }),
  brands: (filter, options) => allPages("/brands?" + queryString(filter), options),
  categories: (filter, options) => allPages("/categories?" + queryString(filter), options),
  registerBrand: (body) => api("/brands", { method: "POST", body }),
  registerCategory: (body) => api("/categories", { method: "POST", body }),
  deleteBrand: (id) => api(`/brands/${id}`, { method: "DELETE" }),
  deleteCategory: (id) => api(`/categories/${id}`, { method: "DELETE" }),
  warehouses: async (options) => {
    const warehouses = [];
    let page = 0;
    let response;
    do {
      response = await api(
        "/warehouses?" + queryString({ active: true, page, size: 100 }),
        options,
      );
      warehouses.push(...response.content);
      page += 1;
    } while (page < response.totalPages);
    return warehouses;
  },
};
