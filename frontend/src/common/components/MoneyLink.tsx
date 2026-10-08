"use client";

import Link, { type LinkProps } from "next/link";
import { usePathname } from "next/navigation";
import type { AnchorHTMLAttributes } from "react";
import { getLocaleFromPathname, isLocale, withMoneyLocale } from "@/i18n/config";

type Props = LinkProps & Omit<AnchorHTMLAttributes<HTMLAnchorElement>, keyof LinkProps> & { children: React.ReactNode };

const moneyRoots = new Set(["", "login", "signup", "forgot-password", "change-required-password", "books", "board", "account", "admin", "privacy", "terms", "forbidden", "not-found"]);

export function localizeMoneyHref(href: string, locale: ReturnType<typeof getLocaleFromPathname>) {
  if (!href.startsWith("/") || href.startsWith("//")) return href;
  const path = href.split(/[?#]/, 1)[0];
  const parts = path.split("/").filter(Boolean);
  if (isLocale(parts[0])) {
    if (parts[1] === "money") return withMoneyLocale(locale, href);
    parts.shift();
  }
  if (parts.length === 0 || moneyRoots.has(parts[0])) return withMoneyLocale(locale, href);
  return href;
}

export default function MoneyLink({ href, ...props }: Props) {
  const pathname = usePathname() || "/ko/money";
  const locale = getLocaleFromPathname(pathname);
  const localizedHref = typeof href === "string" ? localizeMoneyHref(href, locale) : href;
  return <Link href={localizedHref} {...props} />;
}
