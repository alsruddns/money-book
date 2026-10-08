import type { Metadata } from "next";
import type { ReactNode } from "react";
import { headers } from "next/headers";
import { Geist, Geist_Mono } from "next/font/google";
import Script from "next/script";
import StoreProvider from "@/store/StoreProvider";
import GlobalHeader from "@/common/components/GlobalHeader";
import LanguageSelector from "@/common/components/LanguageSelector";
import { SITE_DESCRIPTION, SITE_TITLE } from "@/common/seo/siteMetadata";
import { defaultLocale, isLocale, localeHtmlLang } from "@/i18n/config";
import "./globals.css";

const GA_ID = process.env.NEXT_PUBLIC_GA_ID;

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });
export const metadata: Metadata = {
  metadataBase: new URL("https://www.woori.today"),
  title: SITE_TITLE,
  description: SITE_DESCRIPTION,
  applicationName: "MoneyBook",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const requestHeaders = await headers();
  const requestedLocale = requestHeaders.get("x-locale") ?? defaultLocale;
  const locale = isLocale(requestedLocale) ? requestedLocale : defaultLocale;
  return <html lang={localeHtmlLang[locale]} className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
    <body className="min-h-full flex flex-col">
      {GA_ID && <>
        <Script
          src={`https://www.googletagmanager.com/gtag/js?id=${GA_ID}`}
          strategy="afterInteractive"
        />
        <Script id="google-analytics" strategy="afterInteractive">
          {`window.dataLayer = window.dataLayer || []; function gtag(){dataLayer.push(arguments);} gtag('js', new Date()); gtag('config', '${GA_ID}');`}
        </Script>
      </>}
      <StoreProvider><GlobalHeader /><LanguageSelector />{children}</StoreProvider>
    </body>
  </html>;
}
