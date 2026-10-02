const numberFormatter = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 });

export function formatNumber(value: number | string | null | undefined): string {
  const number = Number(value ?? 0);
  return numberFormatter.format(Number.isFinite(number) ? number : 0);
}

export function formatMoney(amount: number | string | null | undefined): string {
  return `${formatNumber(amount)}원`;
}

export const formatCurrency = formatMoney;

export function formatCount(value: number | string | null | undefined, unit = "건"): string {
  return `${formatNumber(value)}${unit}`;
}
