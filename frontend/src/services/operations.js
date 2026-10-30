import { api } from "./api";
import { queryString } from "./suppliers";

export const warehouses = {
  search: (filter = {}, options) => api("/warehouse/search?" + queryString(filter), options),
  findOne: (id, options) => api(`/warehouse/${id}`, options),
  register: (body) => api("/warehouse", { method: "POST", body }),
  update: (id, body) => api(`/warehouse/${id}`, { method: "PATCH", body }),
  addresses: (warehouseId, options) =>
    api("/warehouseAddress/search?" + queryString({ warehouseId }), options),
  registerAddress: (body) => api("/warehouseAddress", { method: "POST", body }),
  updateAddress: (id, body) => api(`/warehouseAddress/${id}`, { method: "PATCH", body }),
};
export const stockOperations = {
  search: (filter, options) => api("/stockItem/search?" + queryString(filter), options),
  configure: (body) => api("/stockItem", { method: "POST", body }),
  update: (id, body) => api(`/stockItem/${id}`, { method: "PATCH", body }),
  move: (body) => api("/stockMovement", { method: "POST", body }),
  history: (filter, options) => api("/stockMovement/search?" + queryString(filter), options),
};
export const orders = {
  search: (filter, options) => api("/order/search?" + queryString(filter), options),
  findOne: (id, options) => api(`/order/${id}`, options),
  price: (id, productId, presentationId, options) =>
    api(`/order/${id}/price?` + queryString({ productId, presentationId }), options),
  register: (body) => api("/order", { method: "POST", body }),
  update: (id, body) => api(`/order/${id}`, { method: "PATCH", body }),
  items: (orderId, options) => api("/orderItem/search?" + queryString({ orderId }), options),
  addItem: (body) => api("/orderItem", { method: "POST", body }),
  updateItem: (id, body) => api(`/orderItem/${id}`, { method: "PATCH", body }),
  deleteItem: (id) => api(`/orderItem/${id}`, { method: "DELETE" }),
  dispatch: (id) => api(`/order/${id}/dispatch`, { method: "POST", body: {} }),
};
export const purchases = {
  search: (filter, options) => api("/purchase/search?" + queryString(filter), options),
  findOne: (id, options) => api(`/purchase/${id}`, options),
  register: (body) => api("/purchase", { method: "POST", body }),
  update: (id, body) => api(`/purchase/${id}`, { method: "PATCH", body }),
  items: (purchaseId, options) =>
    api("/purchaseItem/search?" + queryString({ purchaseId }), options),
  addItem: (body) => api("/purchaseItem", { method: "POST", body }),
  updateItem: (id, body) => api(`/purchaseItem/${id}`, { method: "PATCH", body }),
  deleteItem: (id) => api(`/purchaseItem/${id}`, { method: "DELETE" }),
  receipts: (id, options) => api(`/purchase/${id}/receipts`, options),
  receive: (id, body) => api(`/purchase/${id}/receive`, { method: "POST", body }),
};
