"use client";

import Link from "next/link";
import { useCurrentUser } from "@/auth/hooks/useCurrentUser";

export function LandingHeader() {
  const { isAuthenticated, isLoading } = useCurrentUser();
  if (isAuthenticated || isLoading) return null;

  return <header className="border-b border-zinc-200 bg-white">
    <div className="mx-auto flex min-h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
      <Link href="/" className="text-lg font-bold tracking-tight text-zinc-900">MoneyBook</Link>
      <nav aria-label="공개 메뉴" className="flex items-center gap-2 sm:gap-4">
        <Link href="#features" className="hidden min-h-11 items-center px-2 text-sm font-medium text-zinc-600 hover:text-blue-700 sm:inline-flex">주요 기능</Link>
        <Link href="/login" className="inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-medium text-zinc-700 hover:bg-zinc-100">로그인</Link>
        <Link href="/signup" className="inline-flex min-h-11 items-center rounded-lg bg-blue-700 px-4 text-sm font-semibold text-white hover:bg-blue-800">무료로 시작하기</Link>
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
