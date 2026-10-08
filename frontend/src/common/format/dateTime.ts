const localDatePattern = /^(\d{4})-(\d{2})-(\d{2})$/;
const hasTimeZone = /(?:Z|[+-]\d{2}:?\d{2})$/i;
type UiLocale = "ko" | "en" | "ja" | "zh";
const intlLocale: Record<UiLocale, string> = { ko: "ko-KR", en: "en-US", ja: "ja-JP", zh: "zh-CN" };

export function formatLocalDate(value: string | null | undefined, fallback = "-", locale: UiLocale = "ko"): string {
  if (!value) return fallback;
  const match = localDatePattern.exec(value);
  if (!match) return value;
  const [, year, month, day] = match;
  if (locale === "ko") return `${year}.${month}.${day}`;
  return new Intl.DateTimeFormat(intlLocale[locale], { year: "numeric", month: "short", day: "numeric", timeZone: "UTC" }).format(new Date(`${year}-${month}-${day}T00:00:00Z`));
}

export function formatKoreaDateTime(value: string | null | undefined, fallback = "-", locale: UiLocale = "ko"): string {
  if (!value) return fallback;
  const normalized = hasTimeZone.test(value) ? value : `${value}+09:00`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return fallback;
  if (locale !== "ko") return new Intl.DateTimeFormat(intlLocale[locale], {
    timeZone: "Asia/Seoul", dateStyle: "medium", timeStyle: "short",
  }).format(date);
  const parts = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(date);
  return `${parts.slice(0, 10).replaceAll("-", ".")} ${parts.slice(11, 16)}`;
}
