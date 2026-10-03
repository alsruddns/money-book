import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import StoreProvider from "@/store/StoreProvider";
import GlobalHeader from "@/common/components/GlobalHeader";
import { createPublicMetadata, SITE_DESCRIPTION, SITE_TITLE } from "@/common/seo/siteMetadata";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = createPublicMetadata("/", SITE_TITLE, SITE_DESCRIPTION);

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <StoreProvider><GlobalHeader />{children}</StoreProvider>
      </body>
    </html>
  );
}
