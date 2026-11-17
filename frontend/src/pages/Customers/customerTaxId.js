export function normalizeTaxId(value) {
  return String(value ?? "").replace(/[.\-/\s]/g, "").toUpperCase();
}

export function validTaxId(value, personType) {
  const document = normalizeTaxId(value);
  const cpf = personType === "PF";
  const length = cpf ? 11 : 14;
  const pattern = cpf ? /^[0-9]{11}$/ : /^[A-Z0-9]{12}[0-9]{2}$/;
  if (!["PF", "PJ"].includes(personType) ||
      !pattern.test(document) || new Set(document).size === 1) return false;
  const digit = (end) => {
    let sum = 0;
    for (let i = 0; i < end; i++) {
      const weight = cpf ? end + 1 - i : (end - 1 - i) % 8 + 2;
      sum += (document.charCodeAt(i) - 48) * weight;
    }
    const remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };
  return digit(length - 2) === Number(document[length - 2]) &&
    digit(length - 1) === Number(document[length - 1]);
}
