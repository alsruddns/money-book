import type { Metadata } from "next";
import { createPublicMetadata, SITE_DESCRIPTION, SITE_NAME } from "./siteMetadata";
import { isLocale, localeHtmlLang, type Locale } from "@/i18n/config";

const descriptions: Record<Locale, string> = {
  ko: "가족과 함께 수입과 지출을 기록하고 생활비를 관리하는 공유 가계부입니다.",
  en: "A shared household finance app to track income, expenses, budgets, and everyday spending together.",
  ja: "家族やパートナーと収入・支出を記録し、家計を管理できる共有家計簿です。",
  zh: "与家人共同记录收入和支出，管理日常开销的共享账本。",
};
const titles: Record<Locale, string> = { ko: "MoneyBook | 가족과 함께 쓰는 공유 가계부", en: "MoneyBook | Shared household finances", ja: "MoneyBook | 家族で使える共有家計簿", zh: "MoneyBook | 家庭共享账本" };

export async function LocaleMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale: raw } = await params;
  const locale = isLocale(raw) ? raw : "ko";
  return { ...createLocalizedMetadata(locale, `/${locale}`, titles[locale], descriptions[locale]), robots: { index: false, follow: false, googleBot: { index: false, follow: false } } };
}

export function createLocalizedMetadata(locale: Locale, pathname: string, title: string, description = SITE_DESCRIPTION): Metadata {
  const metadata = createPublicMetadata(pathname, title, description);
  const languagePaths = Object.fromEntries((["ko", "en", "ja", "zh-CN", "x-default"] as const).map((language) => [language, `/${language === "zh-CN" || language === "x-default" ? "ko" : language}${pathname.replace(/^\/(ko|en|ja|zh)/, "")}`]));
  return { ...metadata, alternates: { ...metadata.alternates, languages: languagePaths }, openGraph: { ...metadata.openGraph, locale: localeHtmlLang[locale].replace("-", "_") }, applicationName: SITE_NAME };
}
