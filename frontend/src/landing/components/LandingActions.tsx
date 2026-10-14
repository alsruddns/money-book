"use client";

import Link from "../../common/components/MoneyLink";
import NextLink from "next/link";
import { usePathname } from "next/navigation";
import { getLocaleFromPathname } from "@/i18n/config";
import { familySitesCopy } from "@/i18n/familySitesCopy";
import { useCurrentUser } from "@/auth/hooks/useCurrentUser";
import FamilySiteSelector from "@/common/components/FamilySiteSelector";
import LanguageSelector from "@/common/components/LanguageSelector";

export function LandingHeader() {
  const locale = getLocaleFromPathname(usePathname() || "/ko");
  const family = familySitesCopy[locale];
  const { isAuthenticated } = useCurrentUser();
  if (isAuthenticated) return null;

  return <header className="border-b border-zinc-200 bg-white">
    <div className="mx-auto flex min-h-16 max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-2 sm:flex-nowrap sm:gap-4 sm:px-6 sm:py-0 lg:px-8">
      <NextLink href={`/${locale}`} aria-label={`woori.today ${locale}`} className="text-lg font-bold tracking-tight text-zinc-900">woori.today</NextLink>
      <nav aria-label={family.label} className="flex w-full flex-wrap items-center justify-end gap-1 sm:w-auto sm:flex-nowrap sm:gap-4">
        <div className="flex shrink-0 items-center gap-1"><FamilySiteSelector locale={locale} className="max-[380px]:w-24 sm:max-w-48" /><LanguageSelector compact /></div>
        <Link href="#features" className="hidden min-h-11 items-center px-2 text-sm font-medium text-zinc-600 hover:text-blue-700 sm:inline-flex">주요 기능</Link>
        <Link href="/login" className="hidden min-h-11 items-center rounded-lg px-3 text-sm font-medium text-zinc-700 hover:bg-zinc-100 sm:inline-flex">로그인</Link>
        <Link href="/signup" className="hidden min-h-11 items-center rounded-lg bg-blue-700 px-4 text-sm font-semibold text-white hover:bg-blue-800 sm:inline-flex">무료로 시작하기</Link>
      </nav>
    </div>
  </header>;
}

export default function LandingActions({ light = false }: { light?: boolean }) {
  const { isAuthenticated, isLoading } = useCurrentUser();
  const buttonClass = light
    ? "inline-flex min-h-12 items-center justify-center rounded-lg bg-white px-5 font-semibold text-blue-950 hover:bg-blue-50"
    : "inline-flex min-h-12 items-center justify-center rounded-lg bg-blue-700 px-5 font-semibold text-white hover:bg-blue-800";

  if (isLoading) {
    return <div aria-label="로그인 상태 확인 중" className="flex min-h-12 flex-wrap items-center gap-3">
      <Link href="/signup" className={buttonClass}>무료로 시작하기</Link>
      <Link href="/login" className="inline-flex min-h-12 items-center justify-center rounded-lg border border-zinc-300 bg-white px-5 font-semibold text-zinc-800 hover:bg-zinc-50">로그인</Link>
    </div>;
  }

  if (isAuthenticated) {
    return <Link href="/books" className={buttonClass}>내 가계부로 이동</Link>;
  }

  return <div className="flex flex-wrap items-center gap-3">
    <Link href="/signup" className={buttonClass}>무료로 시작하기</Link>
    <Link href="/login" className={`inline-flex min-h-12 items-center justify-center rounded-lg border px-5 font-semibold ${light ? "border-white/40 text-white hover:bg-white/10" : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-50"}`}>로그인</Link>
  </div>;
}
