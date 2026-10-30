import { api } from "./api";
import { queryString } from "./suppliers";

export const products = {
  search: (filter, page = 0, size = 25, options) =>
    api("/productCatalog/search?" + queryString({ ...filter, page, size }), options),
  findOne: (id, options) => api(`/productCatalog/${id}`, options),
  register: (body) => api("/productCatalog", { method: "POST", body }),
  update: (id, body) => api(`/productCatalog/${id}`, { method: "PATCH", body }),
  items: (id, filter, page = 0, size = 25, options) =>
    api(`/productCatalog/${id}/items/search?` + queryString({ ...filter, page, size }), options),
  registerItem: (id, body) => api(`/productCatalog/${id}/items`, { method: "POST", body }),
  updateItem: (id, body) => api(`/product/${id}`, { method: "PATCH", body }),
  findItem: (id, options) => api(`/product/${id}`, options),
  presentations: (productId, filter, page = 0, size = 25, options) =>
    api(
      "/productPresentation/search?" + queryString({ ...filter, productId, page, size }),
      options,
    ),
  registerPresentation: (body) => api("/productPresentation", { method: "POST", body }),
  findPresentation: (id, options) => api(`/productPresentation/${id}`, options),
  updatePresentation: (id, body) => api(`/productPresentation/${id}`, { method: "PATCH", body }),
  brands: (filter, options) => api("/brand?" + queryString(filter), options),
  categories: (filter, options) => api("/category?" + queryString(filter), options),
  registerBrand: (body) => api("/brand", { method: "POST", body }),
  registerCategory: (body) => api("/category", { method: "POST", body }),
  deleteBrand: (id) => api(`/brand/${id}`, { method: "DELETE" }),
  deleteCategory: (id) => api(`/category/${id}`, { method: "DELETE" }),
  warehouses: async (options) => {
    const warehouses = [];
    let page = 0;
    let response;
    do {
      response = await api(
        "/warehouse/search?" + queryString({ active: true, page, size: 100 }),
        options,
      );
      warehouses.push(...response.content);
      page += 1;
    } while (page < response.totalPages);
    return warehouses;
  },
};
