"use client";

import { usePathname, useRouter } from "next/navigation";
import { useMemo } from "react";
import { getLocaleFromPathname } from "@/i18n/config";
import { localizeMoneyHref } from "./MoneyLink";

export function useMoneyRouter() {
  const router = useRouter();
  const pathname = usePathname() || "/ko/money";
  const locale = getLocaleFromPathname(pathname);
  return useMemo(() => ({
    ...router,
    push: (href: string, options?: Parameters<typeof router.push>[1]) => router.push(localizeMoneyHref(href, locale), options),
    replace: (href: string, options?: Parameters<typeof router.replace>[1]) => router.replace(localizeMoneyHref(href, locale), options),
  }), [router, locale]);
}
