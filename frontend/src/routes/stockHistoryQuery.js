export const historyTypes = ["INBOUND", "OUTBOUND", "ADJUSTMENT"];
export const historyReasons = ["PURCHASE", "SALE", "RETURN", "MANUAL_ADJUSTMENT", "LOSS"];
export const historyIds = [
  "productId",
  "warehouseId",
  "stockItemId",
  "orderId",
  "purchaseId",
  "userId",
];
export const historyDates = ["occurredFrom", "occurredTo"];
export function parseHistoryQuery(params) {
  const filter = {};
  const errors = [];
  const recognized = [...historyIds, ...historyDates, "type", "reason"];
  for (const [key, value] of params) {
    if (!recognized.includes(key) || params.getAll(key).length !== 1) {
      errors.push("Filtro desconhecido ou repetido.");
      continue;
    }
    if (!value) continue;
    if (historyIds.includes(key)) {
      if (!/^[1-9]\d*$/.test(value) || !Number.isSafeInteger(Number(value)))
        errors.push("Informe identificadores inteiros positivos válidos.");
      else filter[key] = value;
    } else if (historyDates.includes(key)) {
      const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
      const date = new Date(`${value.length === 16 ? value + ":00" : value}Z`);
      if (
        !match ||
        Number.isNaN(date.getTime()) ||
        date.toISOString().slice(0, 19) !== (value.length === 16 ? value + ":00" : value)
      )
        errors.push("Intervalo de datas inválido.");
      else filter[key] = value;
    } else if (!(key === "type" ? historyTypes : historyReasons).includes(value))
      errors.push("Tipo ou motivo de movimento inválido.");
    else filter[key] = value;
  }
  if (filter.occurredFrom && filter.occurredTo && filter.occurredFrom > filter.occurredTo)
    errors.push("O início deve ser anterior ao fim do intervalo.");
  return { filter, error: [...new Set(errors)].join(" ") };
}
