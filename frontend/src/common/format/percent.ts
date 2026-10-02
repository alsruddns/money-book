export function formatPercent(value: number | null | undefined, fractionDigits = 1): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "0.0%";
  return `${value.toFixed(fractionDigits)}%`;
}
