"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useAdminSession } from "../hooks/useAdminSession";

const nav = [
  { href: "/admin", label: "대시보드", exact: true },
  { href: "/admin/users", label: "사용자 관리" },
  { href: "/admin/money-books", label: "가계부 관리" },
  { href: "/admin/activities", label: "전체 활동내역" },
  { href: "/admin/audit-logs", label: "관리자 감사로그", superOnly: true },
];
export default function AdminShell({ children }: { children: React.ReactNode }) {
  const session = useAdminSession(); const pathname = usePathname(); const [open, setOpen] = useState(false);
  if (session.isLoading) return <main className="mx-auto max-w-7xl p-8" aria-live="polite">관리자 권한을 확인하고 있습니다...</main>;
  if (!session.isAuthenticated || !session.isAdmin) return <main className="mx-auto max-w-7xl p-8">관리자 화면으로 이동하고 있습니다...</main>;
  const items = nav.filter((item) => !item.superOnly || session.isSuperAdmin);
  return <div className="min-h-screen bg-zinc-50 text-zinc-900">
    <header className="sticky top-0 z-20 border-b border-zinc-200 bg-white px-4 py-3 md:hidden"><div className="flex items-center justify-between"><button aria-label="관리자 메뉴 열기" aria-expanded={open} onClick={() => setOpen(!open)} className="min-h-11 rounded-lg px-3 font-medium hover:bg-zinc-100">☰ 메뉴</button><span className="font-semibold">관리자 영역</span></div></header>
    {open && <button aria-label="메뉴 닫기" className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={() => setOpen(false)} />}
    <div className="mx-auto flex min-h-screen max-w-[1600px]">
      <aside className={`${open ? "translate-x-0" : "-translate-x-full"} fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-zinc-200 bg-white p-5 transition-transform md:sticky md:top-0 md:h-screen md:translate-x-0`}>
        <div className="mb-7"><p className="text-xs font-semibold uppercase tracking-wide text-blue-700">System Admin</p><p className="mt-2 text-lg font-bold">관리자 영역</p><p className="mt-1 text-sm text-zinc-600">{session.user?.nickname}</p><span className="mt-2 inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-800">{session.user?.systemRole === "SUPER_ADMIN" ? "최고 관리자" : "시스템 관리자"}</span></div>
        <nav aria-label="관리자 메뉴" className="space-y-1">{items.map((item) => { const active = item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`); return <Link key={item.href} href={item.href} aria-current={active ? "page" : undefined} onClick={() => setOpen(false)} className={`block rounded-lg px-3 py-2.5 text-sm font-medium ${active ? "bg-blue-50 text-blue-800" : "text-zinc-700 hover:bg-zinc-100"}`}>{item.label}</Link>; })}</nav>
        <div className="mt-auto space-y-1">
          <Link href="/account" className="block rounded-lg px-3 py-2.5 text-sm font-medium text-zinc-600 hover:bg-zinc-100">계정 관리</Link>
          <Link href="/books" className="block rounded-lg px-3 py-2.5 text-sm font-medium text-zinc-600 hover:bg-zinc-100">가계부 화면으로</Link>
        </div>
      </aside>
      <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8"><div className="mx-auto w-full max-w-6xl">{session.isError ? <section role="alert" className="rounded-xl border border-red-200 bg-white p-6 text-red-800">관리자 권한을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.</section> : children}</div></main>
    </div>
  </div>;
}
