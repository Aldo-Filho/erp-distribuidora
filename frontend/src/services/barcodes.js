import { api } from "./api";
import { queryString } from "./suppliers";
export const barcodes = {
 search: (productId,page=0,options)=>api(`/product-barcodes?${queryString({productId,page,size:25})}`,options),
 lookup: (code,options)=>api(`/product-barcodes/lookup?${queryString({code})}`,options),
 register: body=>api("/product-barcodes",{method:"POST",body}),
 update: (id,body)=>api(`/product-barcodes/${id}`,{method:"PATCH",body}),
};
