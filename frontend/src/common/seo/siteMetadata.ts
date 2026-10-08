import type { Metadata, MetadataRoute } from "next";

export const SITE_NAME = "MoneyBook";
export const SITE_ORIGIN = "https://www.woori.today";
export const SITE_BASE_PATH = "";
export const SITE_TITLE = "무료 공유 가계부";
export const SITE_DESCRIPTION = "가족과 함께 수입과 지출을 기록하고 예산과 소비 흐름을 관리하는 무료 공유 가계부입니다.";

type SiteEnvironment = { NODE_ENV?: string; SITE_URL?: string; GOOGLE_SITE_VERIFICATION?: string; NAVER_SITE_VERIFICATION?: string };

export function getSiteUrl(env: SiteEnvironment = process.env): URL | undefined {
  if (env.NODE_ENV === "production") return new URL(SITE_ORIGIN);
  const configuredUrl = env.SITE_URL?.trim();
  if (!configuredUrl) return new URL("http://localhost:3000");
  const siteUrl = new URL(configuredUrl);
  if (!["http:", "https:"].includes(siteUrl.protocol) || siteUrl.pathname !== "/" || siteUrl.search || siteUrl.hash || siteUrl.username || siteUrl.password) {
    throw new Error("SITE_URL must be an HTTP(S) origin without a path, credentials, query, or fragment.");
  }
  return siteUrl;
}

export function createPublicMetadata(pathname: string, title: string, description: string, env: SiteEnvironment = process.env): Metadata {
  const siteUrl = getSiteUrl(env);
  const canonical = siteUrl ? new URL(`${SITE_BASE_PATH}${pathname === "/" ? "" : pathname}`, siteUrl).toString() : undefined;
  const socialImage = siteUrl ? new URL("/_assets/money/moneybook-og.png", siteUrl).toString() : undefined;
  const googleVerification = env.GOOGLE_SITE_VERIFICATION?.trim();
  const naverVerification = env.NAVER_SITE_VERIFICATION?.trim();
  return {
    title, description, ...(siteUrl ? { metadataBase: siteUrl } : {}), ...(canonical ? { alternates: { canonical } } : {}),
    applicationName: SITE_NAME,
    robots: { index: true, follow: true, googleBot: { index: true, follow: true, "max-image-preview": "large" } },
    openGraph: { type: "website", title, description, siteName: SITE_NAME, locale: "ko_KR", ...(canonical ? { url: canonical } : {}), ...(socialImage ? { images: [{ url: socialImage, width: 1200, height: 630, alt: `${SITE_NAME} shared household budget` }] } : {}) },
    twitter: { card: "summary_large_image", title, description, ...(socialImage ? { images: [socialImage] } : {}) },
    ...(googleVerification || naverVerification ? { verification: { ...(googleVerification ? { google: googleVerification } : {}), ...(naverVerification ? { other: { "naver-site-verification": naverVerification } } : {}) } } : {}),
  };
}

export function privatePageMetadata(): Metadata { return { robots: { index: false, follow: false, googleBot: { index: false, follow: false } } }; }

export function buildWebApplicationJsonLd(env: SiteEnvironment = process.env, locale = "ko") {
  const siteUrl = getSiteUrl(env);
  const canonical = siteUrl ? new URL(`/${locale}/money`, siteUrl).toString() : undefined;
  const localized = {
    ko: { name: "무료 공유 가계부", description: SITE_DESCRIPTION },
    en: { name: "Free Shared Household Budget", description: "A free shared household budget to track income, expenses, and budgets together." },
    ja: { name: "無料共有家計簿", description: "収入・支出や予算を家族と一緒に記録できる無料の共有家計簿です。" },
    zh: { name: "免费共享家庭记账", description: "与家人共同记录收入、支出和预算的免费共享记账工具。" },
  } as const;
  const content = localized[locale as keyof typeof localized] ?? localized.ko;
  return { "@context": "https://schema.org", "@type": "WebApplication", ...content, applicationCategory: "FinanceApplication", operatingSystem: "Web", inLanguage: locale === "zh" ? "zh-CN" : locale, ...(canonical ? { url: canonical } : {}) };
}

export function buildPublicSitemap(env: SiteEnvironment = process.env): MetadataRoute.Sitemap {
  const siteUrl = getSiteUrl(env);
  if (!siteUrl) return [];
  const locales = ["ko", "en", "ja", "zh"] as const;
  return ["", "/privacy", "/terms"].flatMap((suffix) => {
    const languages = { ko: `${SITE_ORIGIN}/ko/money${suffix}`, en: `${SITE_ORIGIN}/en/money${suffix}`, ja: `${SITE_ORIGIN}/ja/money${suffix}`, zh: `${SITE_ORIGIN}/zh/money${suffix}`, "x-default": `${SITE_ORIGIN}/ko/money${suffix}` };
    return locales.map((locale) => ({ url: new URL(`/${locale}/money${suffix}`, siteUrl).toString(), alternates: { languages } }));
  });
}

export function buildRobots(env: SiteEnvironment = process.env): MetadataRoute.Robots {
  const siteUrl = getSiteUrl(env);
  return { rules: { userAgent: "*", allow: "/", disallow: ["/api/", ...["ko", "en", "ja", "zh"].flatMap((locale) => [`/${locale}/money/books/`, `/${locale}/money/account`, `/${locale}/money/admin/`, `/${locale}/money/login`, `/${locale}/money/signup`, `/${locale}/money/forgot-password`])] }, ...(siteUrl ? { sitemap: new URL("/money-sitemap.xml", siteUrl).toString() } : {}) };
}
