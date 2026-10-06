export const warehouseAddressFields = [
  {
    name: "zipCode",
    label: "CEP",
    maxLength: 9,
    placeholder: "Ex.: 01001-000",
  },
  {
    name: "state",
    label: "Estado",
    maxLength: 50,
    placeholder: "Ex.: São Paulo",
  },
  { name: "city", label: "Cidade", maxLength: 100 },
  { name: "neighborhood", label: "Bairro", maxLength: 100 },
  { name: "street", label: "Logradouro", maxLength: 150 },
  {
    name: "number",
    label: "Número",
    maxLength: 10,
    placeholder: "Ex.: 123 ou S/N",
  },
  {
    name: "complement",
    label: "Complemento",
    maxLength: 100,
    placeholder: "Ex.: Galpão 2 ou Sem complemento",
  },
];

export const emptyWarehouseAddress = Object.fromEntries(
  warehouseAddressFields.map(({ name }) => [name, ""]),
);
