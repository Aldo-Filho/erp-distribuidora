export const receiptKey = (userId, purchaseId) => `jamshub:receipt:${userId}:${purchaseId}`;

// Keep the exact request across tab closures. Keys are scoped to the authenticated user and purchase.
export function pendingReceipt(userId, purchaseId) {
  const key = receiptKey(userId, purchaseId);
  try {
    const saved = localStorage.getItem(key);
    if (saved) return JSON.parse(saved);
    // Preserve attempts created before persistent storage was introduced.
    const previous = sessionStorage.getItem(key);
    if (!previous) return null;
    const attempt = JSON.parse(previous);
    localStorage.setItem(key, previous);
    sessionStorage.removeItem(key);
    return attempt;
  } catch {
    // Never silently discard an uncertain attempt or generate a new ID on storage failure.
    throw new Error("Não foi possível acessar a tentativa de recebimento salva neste navegador.");
  }
}

export function savePendingReceipt(userId, purchaseId, attempt) {
  localStorage.setItem(receiptKey(userId, purchaseId), JSON.stringify(attempt));
}

export function clearPendingReceipt(userId, purchaseId) {
  const key = receiptKey(userId, purchaseId);
  localStorage.removeItem(key);
  sessionStorage.removeItem(key);
}
// Display-only subtraction, without converting a commercial amount in the browser.
export function pendingBase(item) {
  if (item.baseQuantity == null || item.receivedQuantity == null) return null;
  const scaled = (value) => {
    const [whole, fraction = ""] = String(value).split(".");
    return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
  };
  const difference = scaled(item.baseQuantity) - scaled(item.receivedQuantity);
  return `${difference / 10n}${difference % 10n ? "," + String(difference % 10n) : ""}`;
}
