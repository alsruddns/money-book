"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import DesktopAdRail from "@/common/components/advertisement/DesktopAdRail";
import { useMoneyBookPermission } from "../hooks/useMoneyBookPermission";
import type { MoneyBookListResponse } from "../dto/res/MoneyBookListResponse";

type MenuItem = { label: string; href: string; disabled?: boolean };
type MenuGroup = { label: string; items: MenuItem[] };

export function getMoneyBookMenu(moneyBookUid: number, canRead: boolean): MenuGroup[] {
  const root = `/books/${moneyBookUid}`;
  const readable = canRead ? [
    { label: "캘린더", href: `${root}/calendar` },
    { label: "거래내역", href: `${root}/transactions` },
    { label: "이체", href: `${root}/transfers` },
  ] : [];
  const management = canRead ? [
    { label: "카테고리", href: `${root}/categories` },
    { label: "계좌/결제수단", href: `${root}/accounts` },
    { label: "예산", href: `${root}/budgets` },
    { label: "정기 수입/지출", href: `${root}/recurring-transactions`, disabled: true },
  ] : [];
  return [
    { label: "주요 메뉴", items: [{ label: "대시보드", href: root }, ...readable] },
    { label: "관리", items: [...management, { label: "멤버 관리", href: `${root}/members` }] },
  ];
}

export function isMoneyBookRouteActive(pathname: string, href: string, root: string): boolean {
  if (href === root) return pathname === root || pathname === `${root}/`;
  return pathname === href || pathname.startsWith(`${href}/`);
}

function NavigationLinks({ groups, pathname, root, onNavigate }: {
  groups: MenuGroup[]; pathname: string; root: string; onNavigate?: () => void;
}) {
  return <nav aria-label="가계부 기능" className="space-y-6">
    {groups.map((group) => <div key={group.label}>
      <p className="mb-2 px-3 text-xs font-semibold text-zinc-500">{group.label}</p>
      <div className="space-y-1">
        {group.items.map((item) => item.disabled ?
          <span key={item.href} aria-disabled="true" className="flex min-h-10 items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm text-zinc-400">
            <span>{item.label}</span><span className="shrink-0 text-xs">준비 중</span>
          </span> :
          <Link key={item.href} href={item.href} onClick={onNavigate}
            aria-current={isMoneyBookRouteActive(pathname, item.href, root) ? "page" : undefined}
            className={`flex min-h-10 items-center rounded-lg px-3 py-2 text-sm font-medium ${isMoneyBookRouteActive(pathname, item.href, root)
              ? "bg-blue-50 text-blue-800" : "text-zinc-700 hover:bg-zinc-100 hover:text-blue-700"}`}>
            {item.label}
          </Link>)}
      </div>
    </div>)}
  </nav>;
}

function SidebarContent({ moneyBook, groups, pathname, onNavigate }: {
  moneyBook: MoneyBookListResponse; groups: MenuGroup[]; pathname: string; onNavigate?: () => void;
}) {
  const root = `/books/${moneyBook.moneyBookUid}`;
  return <div className="flex min-h-full flex-col gap-7 px-4 py-6">
    <div className="min-w-0 px-3">
      <p className="text-xs text-zinc-500">가계부</p>
      <p className="mt-1 break-words text-lg font-semibold">{moneyBook.name}</p>
      {(moneyBook.isOwner || moneyBook.isAdmin) && <span className="mt-2 inline-block rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-900">
        {moneyBook.isOwner ? "소유자" : "관리자"}
      </span>}
    </div>
    <NavigationLinks groups={groups} pathname={pathname} root={root} onNavigate={onNavigate} />
    <Link href="/books" onClick={onNavigate} className="mt-auto rounded-lg px-3 py-2 text-sm font-medium text-zinc-600 hover:bg-zinc-100 hover:text-blue-700">
      ← 가계부 목록으로
    </Link>
  </div>;
}

export default function MoneyBookNavigation({ moneyBookUid, children }: { moneyBookUid: number; children: ReactNode }) {
  const { moneyBook, canRead, isLoading, isError, errorMessage } = useMoneyBookPermission(moneyBookUid);
  const pathname = usePathname();
  const [isDrawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    if (!isDrawerOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setDrawerOpen(false);
    };
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [isDrawerOpen]);

  if (isLoading) return <p role="status">가계부를 불러오는 중...</p>;
  if (isError) return <p role="alert" className="text-red-600">{errorMessage}</p>;
  if (!moneyBook) return <p role="alert">접근 가능한 가계부를 찾을 수 없습니다.</p>;

  const groups = getMoneyBookMenu(moneyBookUid, canRead);
  const sidebar = <SidebarContent moneyBook={moneyBook} groups={groups} pathname={pathname} onNavigate={() => setDrawerOpen(false)} />;

  return <div className="min-w-0">
    <div className="mb-5 flex min-w-0 items-center gap-3 rounded-xl border border-zinc-200 bg-white px-4 py-3 md:hidden">
      <button type="button" aria-label="가계부 메뉴 열기" aria-expanded={isDrawerOpen} aria-controls="moneybook-mobile-menu"
        onClick={() => setDrawerOpen(true)} className="flex size-11 shrink-0 items-center justify-center rounded-lg border border-zinc-200 text-xl hover:bg-zinc-50">
        ☰
      </button>
      <span className="min-w-0 truncate font-semibold">{moneyBook.name}</span>
    </div>
    <div className="grid min-w-0 gap-6 md:grid-cols-[15rem_minmax(0,1fr)]">
      <aside className="hidden self-start rounded-xl border border-zinc-200 bg-white md:sticky md:top-4 md:block md:max-h-[calc(100vh-2rem)] md:overflow-y-auto">
        {sidebar}
      </aside>
      <div className="min-w-0 max-w-full">
        <div className="flex min-w-0 gap-6">
          <div className="min-w-0 max-w-full flex-1 overflow-x-auto">{children}</div>
          <DesktopAdRail />
        </div>
      </div>
    </div>
    {isDrawerOpen && <div className="fixed inset-0 z-50 md:hidden">
      <button type="button" aria-label="가계부 메뉴 닫기" onClick={() => setDrawerOpen(false)} className="absolute inset-0 bg-black/50" />
      <aside id="moneybook-mobile-menu" aria-label="가계부 메뉴" className="relative h-full w-[min(18rem,85vw)] overflow-y-auto bg-white shadow-xl">
        <button type="button" aria-label="가계부 메뉴 닫기" onClick={() => setDrawerOpen(false)} className="absolute right-3 top-3 flex size-10 items-center justify-center rounded-lg text-xl hover:bg-zinc-100">×</button>
        {sidebar}
      </aside>
    </div>}
  </div>;
}
