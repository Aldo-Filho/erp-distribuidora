import { addressKeys, emptyAddress } from "./CustomerAddressFields";

const common = ["legalName", "taxId", "phone", "email", "whatsapp"];
export function customerValues(customer) {
  return {
    personType: customer?.personType || "PF",
    legalName: customer?.legalName || "",
    taxId: customer?.taxId || "",
    tradeName: customer?.tradeName || "",
    birthDate: customer?.birthDate || "",
    municipalReg: customer?.municipalReg || "",
    stateReg: customer?.stateReg || "",
    phone: customer?.phone || "",
    email: customer?.email || "",
    whatsapp: customer?.whatsapp || "",
    active: customer?.active ?? true,
    priceTableId: customer?.priceTable?.id?.toString() || "",
    withAddress: false,
    address: { ...emptyAddress },
  };
}

export function validateAddress(address) {
  if (addressKeys.some((field) => field !== "complement" && !String(address[field] || "").trim())) {
    return "Preencha os campos obrigatórios do endereço.";
  }
  return "";
}

export function validateCustomer(value, editing) {
  if (["legalName", "taxId", "phone"].some((field) => !value[field].trim())) {
    return "Preencha nome, documento e telefone.";
  }
  if (value.personType === "PJ" && !value.tradeName.trim()) return "Informe o nome fantasia.";
  if (value.email && !/^[^\s@]+@[^\s@]+$/.test(value.email)) return "Informe um e-mail válido.";
  const today = new Date();
  const localToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  if (value.personType === "PF" && value.birthDate && value.birthDate > localToday)
    return "Nascimento não pode estar no futuro.";
  return !editing && value.withAddress ? validateAddress(value.address) : "";
}

export function customerPayload(value, customer, canChangePrice) {
  const fields = [
    ...common,
    ...(value.personType === "PJ" ? ["tradeName", "municipalReg", "stateReg"] : ["birthDate"]),
  ];
  const body = {};
  if (!customer) {
    body.personType = value.personType;
    body.active = value.active;
    for (const field of fields) {
      if (field === "birthDate") {
        if (value[field]) body[field] = value[field];
      } else body[field] = value[field];
    }
    if (canChangePrice && value.priceTableId) body.priceTableId = Number(value.priceTableId);
    if (value.withAddress)
      body.address = Object.fromEntries(addressKeys.map((field) => [field, value.address[field]]));
    return body;
  }
  for (const field of fields) {
    if (value[field] !== (customer[field] || "")) {
      if (field === "birthDate" && !value[field]) body.removeBirthDate = true;
      else body[field] = value[field];
    }
  }
  if (canChangePrice && value.priceTableId !== (customer.priceTable?.id?.toString() || "")) {
    if (value.priceTableId) body.priceTableId = Number(value.priceTableId);
    else body.removePriceTable = true;
  }
  return body;
}
