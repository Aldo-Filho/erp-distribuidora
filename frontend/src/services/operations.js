import { api, allPages } from "./api";
import { queryString } from "./suppliers";

async function updateDocument(resource, id, body, transitions) {
  if (body.status != null) {
    const event = transitions[body.status];
    if (!event || Object.keys(body).length !== 1) {
      throw new Error("Altere o estado separadamente dos dados do documento.");
    }
    return api(`${resource}/${id}/${event}`, { method: "POST", body: {} });
  }
  return api(`${resource}/${id}`, { method: "PATCH", body });
}

export const warehouses = {
  search: (filter = {}, options) => api("/warehouses?" + queryString(filter), options),
  findOne: (id, options) => api(`/warehouses/${id}`, options),
  register: (body) => api("/warehouses", { method: "POST", body }),
  update: (id, body) => api(`/warehouses/${id}`, { method: "PATCH", body }),
  addresses: (warehouseId, options) =>
    allPages("/warehouse-addresses?" + queryString({ warehouseId }), options),
  registerAddress: (body) => api("/warehouse-addresses", { method: "POST", body }),
  updateAddress: (id, body) => api(`/warehouse-addresses/${id}`, { method: "PATCH", body }),
};
export const stockOperations = {
  search: (filter, options) => api("/stock-items?" + queryString(filter), options),
  configure: (body) => api("/stock-items", { method: "POST", body }),
  update: (id, body) => api(`/stock-items/${id}`, { method: "PATCH", body }),
  move: (body) => api("/stock-movements", { method: "POST", body }),
  history: (filter, options) => api("/stock-movements?" + queryString(filter), options),
  findMovement: async (stockItemId, requestId, options) => {
    const page = await api(
      "/stock-movements?" + queryString({ stockItemId, requestId, page: 0, size: 1 }),
      options,
    );
    return page.content[0] || null;
  },
};
export const orders = {
  demonstrativeInvoice: (id, options) => api(`/orders/${id}/demonstrative-invoice`, options),
  confirm: (id, body) => api(`/orders/${id}/confirmations`, { method: "POST", body }),
  search: (filter, options) => api("/orders?" + queryString(filter), options),
  findOne: (id, options) => api(`/orders/${id}`, options),
  price: (id, productId, presentationId, options) =>
    api(`/orders/${id}/price?` + queryString({ productId, presentationId }), options),
  register: (body) => api("/orders", { method: "POST", body }),
  update: (id, body) =>
    updateDocument("/orders", id, body, {
      CONFIRMED: "confirmations",
      CANCELED: "cancellations",
    }),
  items: (orderId, options) => allPages("/order-items?" + queryString({ orderId }), options),
  addItem: (body) => api("/order-items", { method: "POST", body }),
  updateItem: (id, body) => api(`/order-items/${id}`, { method: "PATCH", body }),
  deleteItem: (id) => api(`/order-items/${id}`, { method: "DELETE" }),
  dispatch: (id) => api(`/orders/${id}/dispatches`, { method: "POST", body: {} }),
};
export const purchases = {
  search: (filter, options) => api("/purchases?" + queryString(filter), options),
  findOne: (id, options) => api(`/purchases/${id}`, options),
  register: (body) => api("/purchases", { method: "POST", body }),
  update: (id, body) =>
    updateDocument("/purchases", id, body, {
      SENT: "submissions",
      CONFIRMED: "confirmations",
      CANCELED: "cancellations",
    }),
  items: (purchaseId, options) =>
    allPages("/purchase-items?" + queryString({ purchaseId }), options),
  addItem: (body) => api("/purchase-items", { method: "POST", body }),
  updateItem: (id, body) => api(`/purchase-items/${id}`, { method: "PATCH", body }),
  deleteItem: (id) => api(`/purchase-items/${id}`, { method: "DELETE" }),
  receipts: (id, options) => allPages(`/purchases/${id}/receipts`, options),
  receive: (id, body) => api(`/purchases/${id}/receipts`, { method: "POST", body }),
};
