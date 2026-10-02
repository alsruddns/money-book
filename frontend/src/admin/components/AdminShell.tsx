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
  const session = useAdminSession();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const items = nav.filter((item) => !item.superOnly || session.isSuperAdmin);
  const showNavigation = session.isAuthenticated && session.isAdmin;
  return <div className="min-h-screen flex-1 bg-zinc-50 text-zinc-900">
    <header className="sticky top-16 z-20 border-b border-zinc-200 bg-white px-4 py-3 md:hidden"><div className="flex items-center justify-between">
      {showNavigation ? <button aria-label="관리자 메뉴 열기" aria-expanded={open} onClick={() => setOpen(!open)} className="min-h-11 rounded-lg px-3 font-medium hover:bg-zinc-100">메뉴</button> : <span className="min-h-11" />}
      <span className="font-semibold">관리자 영역</span></div></header>
    {open && showNavigation && <button aria-label="메뉴 닫기" className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={() => setOpen(false)} />}
    <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[1600px]">
      <aside className={`${open ? "translate-x-0" : "-translate-x-full"} fixed inset-y-16 left-0 z-40 flex w-72 flex-col border-r border-zinc-200 bg-white p-5 transition-transform md:sticky md:top-16 md:h-[calc(100vh-4rem)] md:translate-x-0`}>
        {showNavigation ? <>
          <div className="mb-7"><p className="text-xs font-semibold uppercase tracking-wide text-blue-700">System Admin</p><p className="mt-2 text-lg font-bold">관리자 영역</p><p className="mt-1 text-sm text-zinc-600">{session.user?.nickname}</p><span className="mt-2 inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-800">{session.user?.systemRole === "SUPER_ADMIN" ? "최고 관리자" : "시스템 관리자"}</span></div>
          <nav aria-label="관리자 메뉴" className="space-y-1">{items.map((item) => { const active = item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`); return <Link key={item.href} href={item.href} aria-current={active ? "page" : undefined} onClick={() => setOpen(false)} className={`block rounded-lg px-3 py-2.5 text-sm font-medium ${active ? "bg-blue-50 text-blue-800" : "text-zinc-700 hover:bg-zinc-100"}`}>{item.label}</Link>; })}</nav>
          <Link href="/books" className="mt-auto block rounded-lg px-3 py-2.5 text-sm font-medium text-zinc-600 hover:bg-zinc-100">가계부 화면으로</Link>
        </> : <div className="animate-pulse space-y-4" aria-hidden="true"><div className="h-5 w-2/3 rounded bg-zinc-100" /><div className="h-10 rounded bg-zinc-100" /></div>}
      </aside>
      <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8"><div className="mx-auto w-full max-w-6xl">
        {session.isLoading ? <section role="status" className="min-h-32 rounded-xl border border-zinc-200 bg-white p-6">관리자 권한을 확인하고 있습니다...</section>
          : session.isError ? <section role="alert" className="rounded-xl border border-red-200 bg-white p-6 text-red-800">관리자 권한을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.</section>
            : !session.isAuthenticated || !session.isAdmin ? <section role="status" className="min-h-32 rounded-xl border border-zinc-200 bg-white p-6">관리자 화면으로 이동하고 있습니다...</section>
              : children}
      </div></main>
    </div>
  </div>;
}
