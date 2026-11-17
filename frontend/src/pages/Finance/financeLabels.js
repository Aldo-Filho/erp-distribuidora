export const paymentLabels = { CASH: "Dinheiro", PIX: "Pix", CREDIT_CARD: "Cartão de crédito", DEBIT_CARD: "Cartão de débito", BOLETO: "Boleto demonstrativo", BANK_TRANSFER: "Transferência bancária" };
export const statusLabels = { OPEN: "Aberta", PAID: "Paga", CANCELED: "Cancelada", PARTIAL: "Parcial (histórico)", OVERDUE: "Vencida (histórico)" };
export const categoryTypes = { REVENUE: "Receita", EXPENSE: "Despesa" };
export const bankTypes = { CASH: "Caixa", CHECKING_ACCOUNT: "Conta corrente", SAVINGS_ACCOUNT: "Poupança" };
export function localDate(offset = 0) {
  const date = new Date(); date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`;
}
export const dateLabel = (value) => value ? value.slice(0,10).split("-").reverse().join("/") : "—";
