"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useSelector } from "react-redux";
import { useLogout } from "@/auth/hooks/useLogout";
import { useGetAccountMeQuery } from "@/account/controller/accountApi";
import type { RootState } from "@/store/store";
import { getGlobalNavItems, isGlobalHeaderHidden } from "./globalNavigation";

export default function GlobalHeader() {
  const pathname = usePathname();
  const [isOpen, setOpen] = useState(false);
  const { accessToken, refreshToken, isInitialized } = useSelector((state: RootState) => state.auth);
  const hasToken = Boolean(accessToken || refreshToken);
  const account = useGetAccountMeQuery(undefined, { skip: !isInitialized || !hasToken });
  const logout = useLogout();
  const role = account.data?.systemRole;
  const hidden = isGlobalHeaderHidden(pathname);

  if (hidden) return null;
  if (!isInitialized) return <header aria-hidden="true" className="min-h-16 border-b border-zinc-200 bg-white" />;
  if (!hasToken) return null;

  const navLinks = getGlobalNavItems(role);
  return <header className="sticky top-0 z-30 border-b border-zinc-200 bg-white/95 text-zinc-900 backdrop-blur">
    <div className="mx-auto flex min-h-16 w-full max-w-screen-2xl items-center justify-between gap-4 px-4 sm:px-6">
      <Link href="/books" className="shrink-0 text-lg font-semibold">가계부</Link>
      <nav aria-label="전역 메뉴" className="hidden items-center gap-1 md:flex">
        {navLinks.map((item) => <Link key={item.href} href={item.href} aria-current={pathname === item.href || (item.href !== "/books" && pathname.startsWith(`${item.href}/`)) ? "page" : undefined}
          className="min-h-11 rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100 hover:text-blue-700">{item.label}</Link>)}
        <button type="button" disabled={logout.isLoading} onClick={() => void logout.logout()} className="min-h-11 rounded-lg px-3 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100 disabled:opacity-50">
          {logout.isLoading ? "로그아웃 중..." : "로그아웃"}
        </button>
      </nav>
      <button type="button" aria-label="전역 메뉴 열기" aria-expanded={isOpen} aria-controls="global-mobile-menu" onClick={() => setOpen((value) => !value)}
        className="flex min-h-11 items-center rounded-lg border border-zinc-300 px-3 text-sm font-medium md:hidden">메뉴</button>
    </div>
    {isOpen && <nav id="global-mobile-menu" aria-label="모바일 전역 메뉴" className="border-t border-zinc-200 bg-white p-3 md:hidden">
      <div className="mx-auto flex max-w-screen-2xl flex-col gap-1 px-1">
        {navLinks.map((item) => <Link key={item.href} href={item.href} onClick={() => setOpen(false)} aria-current={pathname === item.href || (item.href !== "/books" && pathname.startsWith(`${item.href}/`)) ? "page" : undefined}
          className="flex min-h-11 items-center rounded-lg px-3 text-sm font-medium hover:bg-zinc-100">{item.label}</Link>)}
        <button type="button" disabled={logout.isLoading} onClick={() => void logout.logout()} className="min-h-11 rounded-lg px-3 text-left text-sm font-medium hover:bg-zinc-100 disabled:opacity-50">로그아웃</button>
      </div>
    </nav>}
  </header>;
}
