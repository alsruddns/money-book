"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { parseSelectedMonth, shiftMonth } from "../month";

export function useMonthNavigation() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const selectedMonth = parseSelectedMonth(searchParams.get("year"), searchParams.get("month"));

  function moveMonth(offset: -1 | 1) {
    const next = shiftMonth(selectedMonth, offset);
    if (next.year !== selectedMonth.year || next.month !== selectedMonth.month) {
      router.push(`${pathname}?year=${next.year}&month=${next.month}`);
    }
  }

  return { ...selectedMonth, moveMonth };
}
