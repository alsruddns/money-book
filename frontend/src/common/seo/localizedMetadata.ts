import type { Metadata } from "next";
import { createPublicMetadata, SITE_DESCRIPTION, SITE_NAME } from "./siteMetadata";
import { localeHtmlLang, type Locale } from "@/i18n/config";

export function LocaleMetadata(): Metadata {
  return { robots: { index: false, follow: false, googleBot: { index: false, follow: false } } };
}

export function createLocalizedMetadata(locale: Locale, pathname: string, title: string, description = SITE_DESCRIPTION): Metadata {
  const metadata = createPublicMetadata(pathname, title, description);
  const languagePaths = Object.fromEntries((["ko", "en", "ja", "zh-CN", "x-default"] as const).map((language) => [language, `/${language === "zh-CN" || language === "x-default" ? "ko" : language}${pathname.replace(/^\/(ko|en|ja|zh)/, "")}`]));
  return { ...metadata, alternates: { ...metadata.alternates, languages: languagePaths }, openGraph: { ...metadata.openGraph, locale: localeHtmlLang[locale].replace("-", "_") }, applicationName: SITE_NAME };
}
