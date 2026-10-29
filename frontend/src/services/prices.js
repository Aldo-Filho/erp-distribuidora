import { api } from "./api";
import { queryString } from "./customers";

export const prices = {
  search: (filter = {}, options) => api("/price-table/search?" + queryString(filter), options),
};
