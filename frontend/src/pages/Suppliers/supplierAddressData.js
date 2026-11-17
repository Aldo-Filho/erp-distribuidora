export const addressTypes = { MAIN: "Principal", BILLING: "Cobrança", SHIPPING: "Entrega" };
export const emptySupplierAddress = {
  country: "Brasil",
  addressType: "MAIN",
  zipCode: "",
  state: "",
  city: "",
  neighborhood: "",
  street: "",
  number: "",
  complement: "",
};
export const supplierAddressKeys = Object.keys(emptySupplierAddress);

