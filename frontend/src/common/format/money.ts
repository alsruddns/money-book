const formatter = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 });

export function formatMoney(amount: number | string | null | undefined): string {
  const value = Number(amount ?? 0);
  return `${formatter.format(Number.isFinite(value) ? value : 0)}원`;
}
