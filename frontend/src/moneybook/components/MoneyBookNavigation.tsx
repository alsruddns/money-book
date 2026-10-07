"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Link from "../../common/components/MoneyLink";
import { usePathname } from "next/navigation";
import DesktopAdRail from "@/common/components/advertisement/DesktopAdRail";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMoneyBookSettingQuery } from "@/settings/controller/moneyBookSettingApi";
import { useMoneyBookPermission } from "../hooks/useMoneyBookPermission";
import type { MoneyBookListResponse } from "../dto/res/MoneyBookListResponse";
import { translate } from "@/i18n/messages";
import { useTranslation } from "@/i18n/useTranslation";
import type { Locale } from "@/i18n/config";

type MenuItem = { label: string; href: string };
type MenuGroup = { label: string; items: MenuItem[] };
type Permission = { canRead: boolean; isOwner: boolean; isAdmin: boolean };

export function getMoneyBookMenu(moneyBookUid: number, permission: Permission, locale: Locale = "ko"): MenuGroup[] {
  const label = (key: string) => translate(locale, `navigation.${key}`);
  const root = `/books/${moneyBookUid}`;
  const readable = permission.canRead;
  const groups: MenuGroup[] = [
    { label: label("dashboard"), items: [{ label: label("reports"), href: root }] },
    { label: label("book"), items: readable ? [
      { label: label("calendar"), href: `${root}/calendar` },
      { label: label("transactions"), href: `${root}/transactions` },
      { label: label("transfers"), href: `${root}/transfers` },
      { label: label("recurring"), href: `${root}/recurring-transactions` },
    ] : [] },
    { label: label("budget"), items: readable ? [{ label: label("budget"), href: `${root}/budgets` }] : [] },
  ];
  const management: MenuItem[] = readable ? [
    { label: label("categories"), href: `${root}/categories` },
    { label: label("accounts"), href: `${root}/accounts` },
    { label: label("activity"), href: `${root}/activities` },
    { label: label("closing"), href: `${root}/closings` },
    { label: label("settings"), href: `${root}/settings` },
  ] : [];
  if (permission.isOwner || permission.isAdmin) management.push({ label: label("members"), href: `${root}/members` });
  if (management.length > 0) groups.push({ label: label("manage"), items: management });
  return groups.filter((group) => group.items.length > 0);
}

export function isMoneyBookRouteActive(pathname: string, href: string, root: string): boolean {
  if (href === root) return pathname === root || pathname === `${root}/`;
  return pathname === href || pathname.startsWith(`${href}/`);
}

function NavigationLinks({ groups, pathname, root, onNavigate }: {
  groups: MenuGroup[]; pathname: string; root: string; onNavigate?: () => void;
}) {
  const { t } = useTranslation();
  return <nav aria-label={t("navigation.book")} className="space-y-5">
    {groups.map((group) => <section key={group.label}>
      <h2 className="mb-2 rounded-md px-3 py-1 text-sm font-semibold text-zinc-900">{group.label}</h2>
      <div className="space-y-1">{group.items.map((item) => <Link key={item.href} href={item.href} onClick={onNavigate}
        aria-current={isMoneyBookRouteActive(pathname, item.href, root) ? "page" : undefined}
        className={`ml-5 flex min-h-10 items-center rounded-lg px-3 py-2 text-sm font-normal transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 ${isMoneyBookRouteActive(pathname, item.href, root)
          ? "bg-blue-50 text-blue-800" : "text-zinc-700 hover:bg-zinc-100 hover:text-blue-700"}`}>{item.label}</Link>)}</div>
    </section>)}
  </nav>;
}

function SidebarContent({ moneyBook, groups, pathname, onNavigate }: {
  moneyBook: MoneyBookListResponse; groups: MenuGroup[]; pathname: string; onNavigate?: () => void;
}) {
  const { t } = useTranslation();
  const root = `/books/${moneyBook.moneyBookUid}`;
  return <div className="flex min-h-full flex-col gap-6 px-4 py-6">
    <div className="min-w-0 px-3"><p className="text-xs text-zinc-500">{t("navigation.book")}</p><p className="mt-1 break-words text-lg font-semibold">{moneyBook.name}</p>
      {(moneyBook.isOwner || moneyBook.isAdmin) && <span className="mt-2 inline-block rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-900">{t(moneyBook.isOwner ? "books.owner" : "books.administrator")}</span>}
    </div>
    <NavigationLinks groups={groups} pathname={pathname} root={root} onNavigate={onNavigate} />
    <Link href="/books" onClick={onNavigate} className="mt-auto rounded-lg px-3 py-2 text-sm font-medium text-zinc-600 hover:bg-zinc-100 hover:text-blue-700">{t("books.backToList")}</Link>
  </div>;
}

export function getDashboardReportTabs(moneyBookUid: number, pathname: string, locale: Locale = "ko") {
  const root = `/books/${moneyBookUid}`;
  if (pathname !== root && !pathname.startsWith(`${root}/reports`)) return [];
  return [
    { label: translate(locale, "reports.summary"), href: root, active: pathname === root },
    { label: translate(locale, "reports.monthly"), href: `${root}/reports/monthly`, active: pathname === `${root}/reports` || pathname.endsWith("/monthly") },
    { label: translate(locale, "reports.yearly"), href: `${root}/reports/yearly`, active: pathname.endsWith("/yearly") },
    { label: translate(locale, "reports.ranking"), href: `${root}/reports/expense-ranking`, active: pathname.endsWith("/expense-ranking") },
  ];
}

function DashboardReportsTabs({ moneyBookUid, pathname }: { moneyBookUid: number; pathname: string }) {
  const { locale, t } = useTranslation();
  const tabs = getDashboardReportTabs(moneyBookUid, pathname, locale);
  if (tabs.length === 0) return null;
  return <nav aria-label={t("navigation.dashboard")} className="mb-5 flex flex-wrap gap-2">{tabs.map((tab) => <Link key={tab.href} href={tab.href} aria-current={tab.active ? "page" : undefined}
    className={`inline-flex min-h-11 items-center rounded-lg border px-4 text-sm font-medium ${tab.active ? "border-blue-600 bg-blue-50 text-blue-800" : "border-zinc-300 bg-white"}`}>{tab.label}</Link>)}</nav>;
}

function NavigationSkeleton() {
  const { t } = useTranslation();
  return <div className="grid min-w-0 gap-6 md:grid-cols-[15rem_minmax(0,1fr)]" aria-label="가계부 화면 불러오기">
    <aside className="hidden min-h-72 animate-pulse rounded-xl border border-zinc-200 bg-white p-6 md:block"><div className="h-5 w-2/3 rounded bg-zinc-100" /><div className="mt-8 space-y-4">{[1, 2, 3, 4, 5].map((item) => <div key={item} className="h-9 rounded bg-zinc-100" />)}</div></aside>
    <div className="min-h-72 rounded-xl border border-zinc-200 bg-white p-6"><p role="status">{t("books.accessChecking")}</p></div>
  </div>;
}

export default function MoneyBookNavigation({ moneyBookUid, children }: { moneyBookUid: number; children: ReactNode }) {
  const { locale, t } = useTranslation();
  const permission = useMoneyBookPermission(moneyBookUid);
  const { moneyBook, isLoading, isError, errorMessage, canRead, isOwner, isAdmin } = permission;
  const pathname = usePathname();
  const router = useMoneyRouter();
  const access = useGetMoneyBookSettingQuery(moneyBookUid);
  const [isDrawerOpen, setDrawerOpen] = useState(false);
  const drawerRef = useRef<HTMLElement>(null);
  const drawerTriggerRef = useRef<HTMLButtonElement>(null);
  const drawerCloseRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!isDrawerOpen) return;
    const previousOverflow = document.body.style.overflow;
    const trigger = drawerTriggerRef.current;
    document.body.style.overflow = "hidden";
    drawerCloseRef.current?.focus();
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") { event.preventDefault(); setDrawerOpen(false); return; }
      if (event.key !== "Tab" || !drawerRef.current) return;
      const focusable = drawerRef.current.querySelectorAll<HTMLElement>('a[href], button:not([disabled])');
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
      trigger?.focus();
    };
  }, [isDrawerOpen]);

  const accessStatus = typeof access.error === "object" && access.error !== null && "status" in access.error
    ? access.error.status : null;
  const accessDenied = access.isError && accessStatus === 403;
  const accessMissing = access.isError && accessStatus === 404;

  useEffect(() => {
    if (accessDenied) router.replace("/forbidden");
    else if (accessMissing) router.replace("/not-found");
    else if (!access.isLoading && !access.isError && !isLoading && !isError && !moneyBook) {
      router.replace("/forbidden");
    }
  }, [accessDenied, accessMissing, access.isLoading, access.isError, isLoading, isError, moneyBook, router]);

  if (access.isLoading && !access.currentData) return <NavigationSkeleton />;
  if (accessDenied || accessMissing) return <NavigationSkeleton />;
  if (access.isError) return <div role="alert" className="rounded-xl border border-red-200 bg-white p-5 text-red-700">
    <p>{getApiErrorMessage(access.error, t("books.accessChecking"))}</p>
    <button type="button" onClick={() => void access.refetch()} className="mt-3 min-h-11 rounded-lg border px-4 font-medium">{t("common.retry")}</button>
  </div>;
  if (isLoading && !moneyBook) return <NavigationSkeleton />;
  if (isError) return <div className="grid min-w-0 gap-6 md:grid-cols-[15rem_minmax(0,1fr)]"><aside className="hidden rounded-xl border bg-white p-6 md:block" aria-hidden="true" /><p role="alert" className="rounded-xl border border-red-200 bg-white p-5 text-red-700">{errorMessage}</p></div>;
  if (!moneyBook) return <p role="alert" className="rounded-xl border bg-white p-5">{t("books.accessMissing")}</p>;
  if (!canRead) return <div className="grid min-w-0 gap-6 md:grid-cols-[15rem_minmax(0,1fr)]"><aside className="hidden rounded-xl border bg-white p-6 md:block" aria-hidden="true" /><p role="alert" className="rounded-xl border bg-white p-5">{t("books.accessDenied")}</p></div>;

  const groups = getMoneyBookMenu(moneyBookUid, { canRead, isOwner, isAdmin }, locale);
  const sidebar = <SidebarContent moneyBook={moneyBook} groups={groups} pathname={pathname} onNavigate={() => setDrawerOpen(false)} />;
  return <div className="min-w-0">
    <div className="mb-5 flex min-w-0 items-center gap-3 rounded-xl border border-zinc-200 bg-white px-4 py-3 md:hidden">
      <button ref={drawerTriggerRef} type="button" aria-label={t("navigation.openMenu")} aria-expanded={isDrawerOpen} aria-controls="moneybook-mobile-menu" onClick={() => setDrawerOpen(true)} className="flex size-11 shrink-0 items-center justify-center rounded-lg border border-zinc-200 text-sm hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600">{t("navigation.menu")}</button>
      <span className="min-w-0 truncate font-semibold">{moneyBook.name}</span>
    </div>
    <div className="grid min-w-0 gap-6 md:grid-cols-[15rem_minmax(0,1fr)]">
      <aside className="hidden self-start rounded-xl border border-zinc-200 bg-white md:sticky md:top-20 md:block md:max-h-[calc(100vh-6rem)] md:overflow-y-auto">{sidebar}</aside>
      <div className="min-w-0 max-w-full"><div className="flex min-w-0 gap-6"><div className="min-w-0 max-w-full flex-1 overflow-x-auto"><DashboardReportsTabs moneyBookUid={moneyBookUid} pathname={pathname} />{children}</div><DesktopAdRail /></div></div>
    </div>
    {isDrawerOpen && <div className="fixed inset-0 z-50 md:hidden"><button type="button" aria-label="가계부 메뉴 닫기" onClick={() => setDrawerOpen(false)} className="absolute inset-0 bg-black/50" />
      <aside ref={drawerRef} id="moneybook-mobile-menu" role="dialog" aria-modal="true" aria-label={t("navigation.mobileMenu")} className="relative h-full w-[min(18rem,85vw)] overflow-y-auto bg-white shadow-xl">
        <button ref={drawerCloseRef} type="button" aria-label={t("common.close")} onClick={() => setDrawerOpen(false)} className="absolute right-3 top-3 flex size-11 items-center justify-center rounded-lg hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600">{t("common.close")}</button>{sidebar}
      </aside></div>}
  </div>;
}
