export const addressTypes = { MAIN: "Principal", BILLING: "Cobrança", SHIPPING: "Entrega" };
export const emptyAddress = {
  addressType: "MAIN",
  zipCode: "",
  state: "",
  city: "",
  neighborhood: "",
  street: "",
  number: "",
  complement: "",
};
export const addressKeys = Object.keys(emptyAddress);

