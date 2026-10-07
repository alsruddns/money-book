const localDatePattern = /^(\d{4})-(\d{2})-(\d{2})$/;
const hasTimeZone = /(?:Z|[+-]\d{2}:?\d{2})$/i;

export function formatLocalDate(value: string | null | undefined, fallback = "-"): string {
  if (!value) return fallback;
  const match = localDatePattern.exec(value);
  if (!match) return value;
  const [, year, month, day] = match;
  return `${year}.${month}.${day}`;
}

export function formatKoreaDateTime(value: string | null | undefined, fallback = "-"): string {
  if (!value) return fallback;
  const normalized = hasTimeZone.test(value) ? value : `${value}+09:00`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return fallback;
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
