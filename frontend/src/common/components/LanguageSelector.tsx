"use client";

import { usePathname, useRouter } from "next/navigation";
import { getLocaleFromPathname, localeNames, replaceMoneyLocale, supportedLocales } from "@/i18n/config";
import type { RootState } from "@/store/store";
import { useSelector } from "react-redux";
import { useTranslation } from "@/i18n/useTranslation";

export default function LanguageSelector({ compact = false, placement = "floating" }: { compact?: boolean; placement?: "floating" | "header" }) {
  const isHeader = compact || placement === "header";
  const pathname = usePathname() || "/ko/money";
  const router = useRouter();
  const locale = getLocaleFromPathname(pathname);
  const { t } = useTranslation();
  const { accessToken, refreshToken, isInitialized } = useSelector((state: RootState) => state.auth);
  const normalizedPath = pathname.replace(/^\/(ko|en|ja|zh)\/money(?=\/|$)/, "") || "/";
  const isLandingPath = /^\/(ko|en|ja|zh)$/.test(pathname);
  const isAuthPath = normalizedPath === "/login" || normalizedPath === "/signup";
  const keepFloating = isAuthPath || (isLandingPath && (!isInitialized || Boolean(accessToken || refreshToken)));

  if (!isHeader && !keepFloating) return null;

  const select = <select
    aria-label={t("common.language")}
    value={locale}
    onChange={(event) => {
      const target = replaceMoneyLocale(`${pathname}${window.location.search}${window.location.hash}`, event.target.value as typeof locale);
      router.push(target);
    }}
    className={isHeader
      ? "h-11 w-16 max-w-[20vw] rounded-lg border border-zinc-300 bg-white px-1 text-xs font-medium text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 sm:w-20 sm:px-2 sm:text-sm"
      : "min-h-9 max-w-36 rounded-lg border-0 bg-transparent px-2 font-medium text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600"}
  >
    {supportedLocales.map((item) => <option key={item} value={item}>{localeNames[item]}</option>)}
  </select>;

  if (isHeader) return <label className="flex shrink-0 items-center"><span className="sr-only">{t("common.language")}</span>{select}</label>;
  return <label className="fixed bottom-3 right-3 z-40 flex items-center gap-2 rounded-xl border border-zinc-200 bg-white/95 p-2 text-sm shadow-lg sm:bottom-4 sm:right-4">
    <span className="sr-only">{t("common.language")}</span>{select}
  </label>;
}
