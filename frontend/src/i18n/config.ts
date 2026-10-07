export const supportedLocales = ["ko", "en", "ja", "zh"] as const;
export type Locale = (typeof supportedLocales)[number];
export const defaultLocale: Locale = "ko";
export const localeHtmlLang: Record<Locale, string> = { ko: "ko", en: "en", ja: "ja", zh: "zh-CN" };
export const localeIntl: Record<Locale, string> = { ko: "ko-KR", en: "en-US", ja: "ja-JP", zh: "zh-CN" };
export const localeNames: Record<Locale, string> = { ko: "한국어", en: "English", ja: "日本語", zh: "中文" };

export function isLocale(value: string | undefined): value is Locale {
  return supportedLocales.some((locale) => locale === value);
}

export function withLocale(locale: Locale, path: string): string {
  const [pathnameAndQuery, hash] = path.split("#", 2);
  const [pathname, query] = pathnameAndQuery.split("?", 2);
  const cleanPath = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const localizedPath = `/${locale}${cleanPath === "/" ? "" : cleanPath}`;
  return `${localizedPath}${query ? `?${query}` : ""}${hash ? `#${hash}` : ""}`;
}

export function getLocaleFromPathname(pathname: string): Locale {
  const segment = pathname.split("/")[1];
  return isLocale(segment) ? segment : defaultLocale;
}

export function replaceLocale(pathname: string, locale: Locale): string {
  const segments = pathname.split("/");
  if (isLocale(segments[1])) segments[1] = locale;
  else segments.splice(1, 0, locale);
  return segments.join("/") || `/${locale}`;
}
