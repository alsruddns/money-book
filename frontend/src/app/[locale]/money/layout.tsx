import { notFound } from "next/navigation";
import type { ReactNode } from "react";
import { isLocale } from "@/i18n/config";
import { LocaleMetadata } from "@/common/seo/localizedMetadata";

export const generateMetadata = LocaleMetadata;

export default async function LocaleLayout({ children, params }: { children: ReactNode; params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  if (!isLocale(locale)) notFound();
  return children;
}
