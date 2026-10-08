export const supportedLocales = ["ko", "en", "ja", "zh"] as const;
export type Locale = (typeof supportedLocales)[number];
export const defaultLocale: Locale = "ko";
export const localeHtmlLang: Record<Locale, string> = { ko: "ko", en: "en", ja: "ja", zh: "zh-CN" };
export const localeIntl: Record<Locale, string> = { ko: "ko-KR", en: "en-US", ja: "ja-JP", zh: "zh-CN" };
export const localeNames: Record<Locale, string> = { ko: "한국어", en: "English", ja: "日本語", zh: "中文" };

export function isLocale(value: string | undefined): value is Locale {
  return supportedLocales.some((locale) => locale === value);
}

function splitSuffix(path: string) {
  const [pathnameAndQuery, hash] = path.split("#", 2);
  const [pathname, query] = pathnameAndQuery.split("?", 2);
  return { pathname, suffix: `${query ? `?${query}` : ""}${hash ? `#${hash}` : ""}` };
}

function moneyPath(path: string) {
  const { pathname, suffix } = splitSuffix(path);
  const segments = pathname.split("/").filter(Boolean);
  if (isLocale(segments[0])) segments.shift();
  if (segments[0] === "money") segments.shift();
  const rest = segments.length ? `/${segments.join("/")}` : "";
  return { rest, suffix };
}

export function withMoneyLocale(locale: Locale, path: string): string {
  const { rest, suffix } = moneyPath(path.startsWith("/") ? path : `/${path}`);
  return `/${locale}/money${rest}${suffix}`;
}

export function getLocaleFromPathname(pathname: string): Locale {
  const segment = pathname.split("/")[1];
  return isLocale(segment) ? segment : defaultLocale;
}

export function replaceMoneyLocale(path: string, locale: Locale): string {
  return withMoneyLocale(locale, path);
}

/** Retained for non-MoneyBook call sites; MoneyBook routes should use withMoneyLocale. */
export function withLocale(locale: Locale, path: string): string {
  const { pathname, suffix } = splitSuffix(path);
  const clean = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const segments = clean.split("/").filter(Boolean);
  if (isLocale(segments[0])) segments[0] = locale;
  else segments.unshift(locale);
  return `/${segments.join("/")}${suffix}`;
}

export const replaceLocale = replaceMoneyLocale;
