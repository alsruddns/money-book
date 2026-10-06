export function parsePositiveAmount(text: string): number | null {
  const cleaned = text.trim().replaceAll(",", "");
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(cleaned)) return null;
  const amount = Number(cleaned);
  if (amount <= 0 || !Number.isSafeInteger(Math.round(amount * 100))) return null;
  return amount;
}
