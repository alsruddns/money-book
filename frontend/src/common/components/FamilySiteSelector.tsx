"use client";

import { useId } from "react";
import { familySitesCopy } from "@/i18n/familySitesCopy";
import type { Locale } from "@/i18n/config";

export default function FamilySiteSelector({ locale, className = "" }: { locale: Locale; className?: string }) {
  const family = familySitesCopy[locale];
  const newTabDescriptionId = useId();
  const sites = [
    { href: `/${locale}`, label: family.portal },
    { href: `/${locale}/calculators`, label: family.calculator },
    { href: `/${locale}/tools`, label: family.tools },
    { href: `/${locale}/money`, label: family.moneyBook },
  ];

  return <>
    <span id={newTabDescriptionId} className="sr-only">{family.newTab}</span>
    <select
      aria-label={family.label}
      aria-describedby={newTabDescriptionId}
      title={family.label}
      defaultValue=""
      onChange={(event) => {
        const href = event.currentTarget.value;
        if (href !== "") window.open(href, "_blank", "noopener,noreferrer");
        event.currentTarget.value = "";
      }}
      className={`h-11 w-[8rem] max-w-[39vw] rounded-lg border border-zinc-300 bg-white px-2 text-sm font-medium text-zinc-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 ${className}`}
    >
      <option value="" disabled>{family.label}</option>
      {sites.map((site) => <option key={site.href} value={site.href}>{site.label}</option>)}
    </select>
  </>;
}
