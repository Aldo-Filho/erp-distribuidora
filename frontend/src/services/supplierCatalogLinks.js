import { api } from "./api";

export const supplierCatalogLinks = {
  brands: (id, options) => api(`/supplier/${id}/brands`, options),
  categories: (id, options) => api(`/supplier/${id}/categories`, options),
  registerBrand: (id, brandId) =>
    api(`/supplier/${id}/brands`, { method: "POST", body: { brandId } }),
  registerCategory: (id, categoryId) =>
    api(`/supplier/${id}/categories`, { method: "POST", body: { categoryId } }),
  deleteBrand: (id, brandId) => api(`/supplier/${id}/brands/${brandId}`, { method: "DELETE" }),
  deleteCategory: (id, categoryId) =>
    api(`/supplier/${id}/categories/${categoryId}`, { method: "DELETE" }),
};
