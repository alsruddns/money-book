export function formatPercent(value: number | null | undefined, fractionDigits = 1): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "0.0%";
  return `${value.toFixed(fractionDigits)}%`;
}

/** Formats a ratio where 1 represents 100 percent (for example, 0.34 → 34.0%). */
export function formatFractionPercent(value: number | null | undefined, fractionDigits = 1): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "-";
  return `${(value * 100).toFixed(fractionDigits)}%`;
}

/** Formats a value already expressed in percentage points (for example, 34 → 34.0%). */
export function formatPercentPoints(value: number | null | undefined, fractionDigits = 1): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "-";
  return `${value.toFixed(fractionDigits)}%`;
}
