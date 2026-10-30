import { api } from "./api";
import { queryString } from "./suppliers";

export const productStock = {
  search: async (productId, options) => {
    const stocks = [];
    let page = 0;
    let response;
    do {
      response = await api(
        "/stockItem/search?" + queryString({ productId, page, size: 100 }),
        options,
      );
      stocks.push(...response.content);
      page += 1;
    } while (page < response.totalPages);
    return stocks;
  },
};
