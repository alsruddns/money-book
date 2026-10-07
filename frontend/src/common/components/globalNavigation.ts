export type GlobalSystemRole = "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN" | string;
import { translate } from "@/i18n/messages";
import type { Locale } from "@/i18n/config";

export function getGlobalNavItems(role: GlobalSystemRole | null | undefined, locale: Locale = "ko") {
  const base = [
    { href: "/books", label: translate(locale, "navigation.myBooks") },
    { href: "/books/invitations", label: translate(locale, "navigation.invitations") },
    { href: "/board", label: translate(locale, "navigation.board") },
    { href: "/account", label: translate(locale, "navigation.account") },
  ];
  return role === "SYSTEM_ADMIN" || role === "SUPER_ADMIN"
    ? [...base.slice(0, 3), { href: "/admin", label: translate(locale, "navigation.admin") }, base[3]]
    : base;
}

export function isGlobalHeaderHidden(pathname: string): boolean {
  const normalized = pathname.replace(/^\/(ko|en|ja|zh)(?=\/|$)/, "") || "/";
  return normalized === "/" || normalized === "/login" || normalized === "/signup";
}

export function isGlobalNavItemActive(pathname: string, href: string): boolean {
  pathname = pathname.replace(/^\/(ko|en|ja|zh)(?=\/|$)/, "") || "/";
  if (href === "/books") return pathname === href || (pathname.startsWith("/books/") && pathname !== "/books/invitations");
  return pathname === href || pathname.startsWith(`${href}/`);
}
