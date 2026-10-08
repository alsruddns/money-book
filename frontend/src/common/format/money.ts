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
type Locale = "ko" | "en" | "ja" | "zh";
const localeIntl: Record<Locale, string> = { ko: "ko-KR", en: "en-US", ja: "ja-JP", zh: "zh-CN" };

/** Formats a display value for the selected UI language without changing its currency unit. */
export function formatLocalizedAmount(amount: number, locale: Locale, currency = "KRW"): string {
  return new Intl.NumberFormat(localeIntl[locale], { style: "currency", currency, maximumFractionDigits: 0 }).format(amount);
}

/** Formats a date for the selected UI language while preserving the supplied date value. */
export function formatLocalizedDate(date: Date, locale: Locale, options?: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(localeIntl[locale], options).format(date);
}
