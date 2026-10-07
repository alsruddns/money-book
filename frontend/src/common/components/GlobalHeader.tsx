"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useSelector } from "react-redux";
import { useLogout } from "@/auth/hooks/useLogout";
import { useGetCurrentUserQuery } from "@/auth/controller/authApi";
import type { RootState } from "@/store/store";
import { getGlobalNavItems, isGlobalHeaderHidden, isGlobalNavItemActive } from "./globalNavigation";
import { getLocaleFromPathname, withLocale } from "@/i18n/config";
import { useTranslation } from "@/i18n/useTranslation";

export default function GlobalHeader() {
  const pathname = usePathname();
  const locale = getLocaleFromPathname(pathname ?? "/ko");
  const { t } = useTranslation();
  const [isOpen, setOpen] = useState(false);
  const { accessToken, refreshToken, isInitialized } = useSelector((state: RootState) => state.auth);
  const hasToken = Boolean(accessToken || refreshToken);
  const currentUser = useGetCurrentUserQuery(undefined, { skip: !isInitialized || !hasToken });
  const logout = useLogout();
  const role = currentUser.data?.systemRole;
  const hidden = isGlobalHeaderHidden(pathname);

  if (hidden) return null;
  if (!isInitialized) return <header aria-hidden="true" className="min-h-16 border-b border-zinc-200 bg-white" />;
  if (!hasToken) return null;

  const navLinks = getGlobalNavItems(role, locale);
  const roleLoading = currentUser.isLoading && !currentUser.data;
  const linkClass = (href: string) => `min-h-11 rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 ${isGlobalNavItemActive(pathname, href) ? "bg-blue-50 text-blue-800" : "text-zinc-700 hover:bg-zinc-100 hover:text-blue-700 active:bg-zinc-200"}`;
  return <header className="sticky top-0 z-30 border-b border-zinc-200 bg-white/95 text-zinc-900 backdrop-blur">
    <div className="mx-auto flex min-h-16 w-full max-w-screen-2xl items-center justify-between gap-4 px-4 sm:px-6">
      <Link href={withLocale(locale, "/")} aria-label={`woori.today ${t("common.home")}`} className="shrink-0">
        <Image src="/images/brand/woori-logo.png" alt="woori.today" width={419} height={99} sizes="(max-width: 640px) 136px, 160px" className="h-auto w-[136px] sm:w-[160px]" />
      </Link>
      <nav aria-label={t("navigation.manage")} aria-busy={roleLoading} className="hidden items-center gap-1 md:flex">
        {roleLoading ? <div aria-hidden="true" className="flex gap-2 px-2"><span className="h-9 w-24 animate-pulse rounded-lg bg-zinc-100" /><span className="h-9 w-24 animate-pulse rounded-lg bg-zinc-100" /><span className="h-9 w-20 animate-pulse rounded-lg bg-zinc-100" /><span className="h-9 w-20 animate-pulse rounded-lg bg-zinc-100" /></div> : navLinks.map((item) => <Link key={item.href} href={withLocale(locale, item.href)} aria-current={isGlobalNavItemActive(pathname, item.href) ? "page" : undefined} className={linkClass(item.href)}>{item.label}</Link>)}
        <button type="button" disabled={logout.isLoading} onClick={() => void logout.logout()} className="min-h-11 cursor-pointer rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-100 hover:text-blue-700 active:bg-zinc-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 disabled:cursor-not-allowed disabled:opacity-50">
          {logout.isLoading ? t("auth.loggingOut") : t("auth.logout")}
        </button>
      </nav>
      <button type="button" aria-label={t("navigation.openMenu")} aria-expanded={isOpen} aria-controls="global-mobile-menu" aria-busy={roleLoading} onClick={() => setOpen((value) => !value)}
        className="flex min-h-11 items-center rounded-lg border border-zinc-300 px-3 text-sm font-medium md:hidden">{t("navigation.menu")}</button>
    </div>
    {isOpen && <nav id="global-mobile-menu" aria-label={t("navigation.mobileMenu")} className="border-t border-zinc-200 bg-white p-3 md:hidden">
      <div className="mx-auto flex max-w-screen-2xl flex-col gap-1 px-1">
        {roleLoading ? <p role="status" className="px-3 py-3 text-sm text-zinc-600">{t("common.loading")}</p> : navLinks.map((item) => <Link key={item.href} href={withLocale(locale, item.href)} onClick={() => setOpen(false)} aria-current={isGlobalNavItemActive(pathname, item.href) ? "page" : undefined} className={`flex min-h-11 items-center ${linkClass(item.href)}`}>{item.label}</Link>)}
        <button type="button" disabled={logout.isLoading} onClick={() => void logout.logout()} className="min-h-11 cursor-pointer rounded-lg px-3 text-left text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-100 hover:text-blue-700 active:bg-zinc-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 disabled:cursor-not-allowed disabled:opacity-50">{logout.isLoading ? t("auth.loggingOut") : t("auth.logout")}</button>
      </div>
    </nav>}
  </header>;
}
