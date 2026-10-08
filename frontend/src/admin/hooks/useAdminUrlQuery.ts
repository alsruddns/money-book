"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";
import { usePathname, useSearchParams } from "next/navigation";
export function useAdminUrlQuery() {
  const params = useSearchParams(); const pathname = usePathname(); const router = useMoneyRouter();
  const pageRaw = Number(params.get("page") ?? 0); const sizeRaw = Number(params.get("size") ?? 20);
  const page = Number.isInteger(pageRaw) && pageRaw >= 0 ? pageRaw : 0; const size = [20, 50, 100].includes(sizeRaw) ? sizeRaw : 20;
  function update(key: string, value: string) { const next = new URLSearchParams(params.toString()); if (value) next.set(key, value); else next.delete(key); next.delete("page"); router.push(next.size ? `${pathname}?${next}` : pathname); }
  function reset() { router.push(pathname); }
  function setPage(value: number) { const next = new URLSearchParams(params.toString()); if (value > 0) next.set("page", String(value)); else next.delete("page"); router.push(`${pathname}?${next}`); }
  return { params, page, size, update, reset, setPage };
}
