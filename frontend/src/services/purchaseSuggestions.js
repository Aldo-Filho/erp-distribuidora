import { api } from "./api";
export const purchaseSuggestions = {
 search:(page=0,options)=>api(`/purchase-suggestions?page=${page}&size=25`,options),
 items:(id,page=0,options)=>api(`/purchase-suggestions/${id}/items?page=${page}&size=25`,options),
 generate:requestId=>api("/purchase-suggestions",{method:"POST",body:{requestId}}),
};
