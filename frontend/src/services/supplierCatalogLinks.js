import { api, allPages } from "./api";

export const supplierCatalogLinks = {
  brands: (id, options) => allPages(`/suppliers/${id}/brands`, options),
  categories: (id, options) => allPages(`/suppliers/${id}/categories`, options),
  registerBrand: (id, brandId) =>
    api(`/suppliers/${id}/brands`, { method: "POST", body: { brandId } }),
  registerCategory: (id, categoryId) =>
    api(`/suppliers/${id}/categories`, { method: "POST", body: { categoryId } }),
  deleteBrand: (id, brandId) => api(`/suppliers/${id}/brands/${brandId}`, { method: "DELETE" }),
  deleteCategory: (id, categoryId) =>
    api(`/suppliers/${id}/categories/${categoryId}`, { method: "DELETE" }),
};
