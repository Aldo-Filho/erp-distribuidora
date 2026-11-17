export function available(stock) {
  const scaled = (value) => {
    const [whole, fraction = ""] = String(value).split(".");
    return BigInt(whole) * 10n + BigInt(fraction.padEnd(1, "0"));
  };
  const difference = scaled(stock.quantity) - scaled(stock.reservedQuantity);
  return `${difference / 10n}${difference % 10n ? "," + String(difference % 10n) : ""}`;
}
