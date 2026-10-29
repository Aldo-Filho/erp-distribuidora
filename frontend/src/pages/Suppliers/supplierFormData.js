import { supplierAddressKeys, emptySupplierAddress } from "./SupplierAddressFields";

const fields = ["legalName", "taxId", "tradeName", "phone", "email", "whatsapp"];
export function supplierValues(supplier) {
  return {
    ...Object.fromEntries(fields.map((field) => [field, supplier?.[field] || ""])),
    avgDeliveryDays: supplier?.avgDeliveryDays?.toString() ?? "",
    active: supplier?.active ?? true,
    withAddress: false,
    address: { ...emptySupplierAddress },
  };
}

export function validateAddress(address) {
  return supplierAddressKeys.some((field) =>
    field !== "complement" && !String(address[field] || "").trim())
    ? "Preencha os campos obrigatórios do endereço." : "";
}

export function validateSupplier(value, editing) {
  if (["legalName", "taxId", "phone"].some((field) => !value[field].trim()))
    return "Preencha razão social, CNPJ e telefone.";
  if (value.email && !/^[^\s@]+@[^\s@]+$/.test(value.email))
    return "Informe um e-mail válido.";
  if (value.avgDeliveryDays !== "" &&
    (!/^\d+$/.test(value.avgDeliveryDays) || Number(value.avgDeliveryDays) > 2147483647))
    return "Informe um prazo inteiro não negativo.";
  return !editing && value.withAddress ? validateAddress(value.address) : "";
}

export function supplierPayload(value, supplier) {
  const body = {};
  for (const field of fields) {
    if (!supplier || value[field] !== (supplier[field] || "")) body[field] = value[field];
  }
  if (!supplier || value.avgDeliveryDays !== (supplier.avgDeliveryDays?.toString() ?? "")) {
    if (value.avgDeliveryDays !== "") body.avgDeliveryDays = Number(value.avgDeliveryDays);
    else if (supplier?.avgDeliveryDays != null) body.removeAvgDeliveryDays = true;
  }
  if (!supplier) {
    body.active = value.active;
    if (value.withAddress) {
      body.address = Object.fromEntries(
        supplierAddressKeys.map((field) => [field, value.address[field]]),
      );
    }
  }
  return body;
}
