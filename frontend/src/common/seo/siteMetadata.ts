import type { Metadata, MetadataRoute } from "next";

export const SITE_NAME = "MoneyBook";
export const SITE_ORIGIN = "https://www.woori.today";
export const SITE_BASE_PATH = "/money";
export const SITE_TITLE = "MoneyBook | 가족과 함께 쓰는 공유 가계부";
export const SITE_DESCRIPTION =
  "가족과 함께 수입과 지출을 기록하고, 캘린더·예산·월간 분석으로 생활비를 관리하는 공유 가계부입니다.";

type SiteEnvironment = {
  NODE_ENV?: string;
  SITE_URL?: string;
  GOOGLE_SITE_VERIFICATION?: string;
  NAVER_SITE_VERIFICATION?: string;
};

/** Builds canonical URLs from the public origin and the deployed Next.js base path. */
export function getSiteUrl(env: SiteEnvironment = process.env): URL | undefined {
  const configuredUrl = env.SITE_URL?.trim();
  if (!configuredUrl) {
    return env.NODE_ENV === "production" ? new URL(SITE_ORIGIN) : new URL("http://localhost:3000");
  }

  const siteUrl = new URL(configuredUrl);
  if (
    !["http:", "https:"].includes(siteUrl.protocol) ||
    siteUrl.pathname !== "/" ||
    siteUrl.search ||
    siteUrl.hash ||
    siteUrl.username ||
    siteUrl.password
  ) {
    throw new Error("SITE_URL must be an HTTP(S) origin without a path, credentials, query, or fragment.");
  }
  return siteUrl;
}

export function createPublicMetadata(
  pathname: string,
  title: string,
  description: string,
  env: SiteEnvironment = process.env,
): Metadata {
  const siteUrl = getSiteUrl(env);
  const canonical = siteUrl ? new URL(`${SITE_BASE_PATH}${pathname === "/" ? "" : pathname}`, siteUrl).toString() : undefined;
  const socialImage = siteUrl ? new URL(`${SITE_BASE_PATH}/moneybook-og.png`, siteUrl).toString() : undefined;
  const googleVerification = env.GOOGLE_SITE_VERIFICATION?.trim();
  const naverVerification = env.NAVER_SITE_VERIFICATION?.trim();

  return {
    title,
    description,
    ...(siteUrl ? { metadataBase: siteUrl } : {}),
    ...(canonical ? { alternates: { canonical } } : {}),
    applicationName: SITE_NAME,
    robots: { index: true, follow: true, googleBot: { index: true, follow: true, "max-image-preview": "large" } },
    openGraph: {
      type: "website",
      title,
      description,
      siteName: SITE_NAME,
      locale: "ko_KR",
      ...(canonical ? { url: canonical } : {}),
      ...(socialImage ? { images: [{ url: socialImage, width: 1200, height: 630, alt: `${SITE_NAME} 공유 가계부` }] } : {}),
    },
    twitter: { card: "summary_large_image", title, description, ...(socialImage ? { images: [socialImage] } : {}) },
    ...(googleVerification || naverVerification
      ? { verification: { ...(googleVerification ? { google: googleVerification } : {}), ...(naverVerification ? { other: { "naver-site-verification": naverVerification } } : {}) } }
      : {}),
  };
}

export function privatePageMetadata(): Metadata {
  return { robots: { index: false, follow: false, googleBot: { index: false, follow: false } } };
}

export function buildWebApplicationJsonLd(env: SiteEnvironment = process.env) {
  const siteUrl = getSiteUrl(env);
  const canonical = siteUrl ? new URL(SITE_BASE_PATH, siteUrl).toString() : undefined;
  return {
    "@context": "https://schema.org",
    "@type": "WebApplication",
    name: SITE_NAME,
    description: SITE_DESCRIPTION,
    applicationCategory: "FinanceApplication",
    operatingSystem: "Web",
    ...(canonical ? { url: canonical } : {}),
  };
}

export function buildPublicSitemap(env: SiteEnvironment = process.env): MetadataRoute.Sitemap {
  const siteUrl = getSiteUrl(env);
  if (!siteUrl) return [];

  return ["/", "/privacy", "/terms"].map((pathname) => ({
    url: new URL(`${SITE_BASE_PATH}${pathname === "/" ? "" : pathname}`, siteUrl).toString(),
  }));
}

export function buildRobots(env: SiteEnvironment = process.env): MetadataRoute.Robots {
  const siteUrl = getSiteUrl(env);
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      // Let crawlers read noindex metadata on member pages; only API endpoints are blocked.
      disallow: ["/api/"],
    },
    ...(siteUrl ? { sitemap: new URL(`${SITE_BASE_PATH}/sitemap.xml`, siteUrl).toString() } : {}),
  };
}
