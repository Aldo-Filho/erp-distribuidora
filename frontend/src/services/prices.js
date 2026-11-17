import { allPages } from "./api";
import { queryString } from "./customers";

export const prices = {
  search: (filter = {}, options) => allPages("/price-tables?" + queryString(filter), options),
};
