import React from "react";
const roots = new Set(["", "login", "signup", "forgot-password", "change-required-password", "books", "board", "account", "admin", "privacy", "terms", "forbidden", "not-found"]);
export function resolveLocaleTestImport(name, mocks, fallback) {
  if (name.endsWith("useMoneyRouter")) {
    return { useMoneyRouter: () => { const router = mocks["next/navigation"]?.useRouter?.() ?? { push() {}, replace() {} }; return { ...router, push: (path) => router.push(`/ko/money${path}`), replace: (path) => router.replace(`/ko/money${path}`) }; } };
  }
  if (name.endsWith("MoneyLink")) {
    return { default: function TestMoneyLink({ href, children, ...props }) {
      if (typeof href !== "string") return React.createElement("a", { href, ...props }, children);
      const [pathname, ...suffix] = href.split(/(?=[?#])/);
      const parts = pathname.split("/").filter(Boolean);
      const hasLocale = ["ko", "en", "ja", "zh"].includes(parts[0] ?? "");
      const isMoneyPath = hasLocale ? parts[1] === "money" || roots.has(parts[1] ?? "") : roots.has(parts[0] ?? "");
      const rest = parts.slice(hasLocale ? (parts[1] === "money" ? 2 : 1) : 0);
      const target = isMoneyPath ? `/ko/money${rest.length ? `/${rest.join("/")}` : ""}${suffix.join("")}` : href;
      return React.createElement("a", { href: target, ...props }, children);
    } };
  }
  if (name === "@/i18n/config") {
    return { getLocaleFromPathname: (pathname) => pathname.split("/")[1] || "ko", withMoneyLocale: (locale, path) => `/${locale}/money${path.startsWith("/") ? path : `/${path}`}` };
  }
  if (name === "@/i18n/useTranslation") return { useTranslation: () => ({ locale: "ko", t: (key) => key === "dashboard.over" ? "\uCD08\uACFC" : key === "dashboard.allRankings" ? "\uC804\uCCB4 \uC9C0\uCD9C \uC21C\uC704 \uBCF4\uAE30" : key }) };
  return fallback(name);
}
