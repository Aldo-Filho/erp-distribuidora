import { characteristics } from "../Products/productFormData.js";

export const partyName = (record) => record?.tradeName || record?.legalName || "—";
export const skuName = (item) =>
  `${item.sku} · ${item.name}${
    Object.entries(item.attributes || {}).length
      ? " · " +
        Object.entries(item.attributes)
          .map(([key, value]) => `${characteristics[key] || key}: ${value}`)
          .join(" / ")
      : ""
  }`;
