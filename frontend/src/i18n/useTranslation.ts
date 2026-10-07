"use client";

import { usePathname } from "next/navigation";
import { getLocaleFromPathname, type Locale } from "./config";
import { translate } from "./messages";

export function useTranslation() {
  const pathname = usePathname() ?? "/ko";
  const locale = getLocaleFromPathname(pathname);
  return { locale, t: (key: string) => translate(locale, key) } as { locale: Locale; t: (key: string) => string };
}
