"use client";

import { usePathname, useRouter } from "next/navigation";
import { getLocaleFromPathname, localeNames, replaceMoneyLocale, supportedLocales } from "@/i18n/config";

export default function LanguageSelector() {
  const pathname = usePathname() || "/ko/money";
  const router = useRouter();
  const locale = getLocaleFromPathname(pathname);
  return <label className="fixed bottom-3 right-3 z-40 flex items-center gap-2 rounded-xl border border-zinc-200 bg-white/95 p-2 text-sm shadow-lg sm:bottom-4 sm:right-4">
    <span className="sr-only">{localeNames[locale]}</span>
    <select aria-label="Language / 언어 선택" value={locale} onChange={(event) => { const target = replaceMoneyLocale(`${pathname}${window.location.search}${window.location.hash}`, event.target.value as typeof locale); router.push(target); }} className="min-h-9 max-w-36 rounded-lg border-0 bg-transparent px-2 font-medium text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600">
      {supportedLocales.map((item) => <option key={item} value={item}>{localeNames[item]}</option>)}
    </select>
  </label>;
}
