import type { Metadata } from "next";
import { createPublicMetadata, SITE_DESCRIPTION, SITE_NAME, SITE_ORIGIN } from "./siteMetadata";
import { localeHtmlLang, type Locale } from "@/i18n/config";

export function LocaleMetadata(): Metadata {
  return { robots: { index: false, follow: false, googleBot: { index: false, follow: false } } };
}

export function createLocalizedMetadata(locale: Locale, pathname: string, title: string, description = SITE_DESCRIPTION): Metadata {
  const metadata = createPublicMetadata(pathname, title, description);
  const suffix = pathname.replace(/^\/(ko|en|ja|zh)\/money/, "");
  const languages = {
    ko: `${SITE_ORIGIN}/ko/money${suffix}`,
    en: `${SITE_ORIGIN}/en/money${suffix}`,
    ja: `${SITE_ORIGIN}/ja/money${suffix}`,
    "zh-CN": `${SITE_ORIGIN}/zh/money${suffix}`,
    "x-default": `${SITE_ORIGIN}/ko/money${suffix}`,
  };
  return {
    ...metadata,
    alternates: { ...metadata.alternates, languages },
    openGraph: { ...metadata.openGraph, locale: localeHtmlLang[locale].replace("-", "_"), url: `${SITE_ORIGIN}${pathname}` },
    applicationName: SITE_NAME,
  };
}
