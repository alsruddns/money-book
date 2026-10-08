"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { usePathname, useSearchParams } from "next/navigation";
import { parseSelectedMonth, shiftMonth } from "../month";

export function useMonthNavigation(minYear = 1, maxYear = 9999) {
  const router = useMoneyRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const selectedMonth = parseSelectedMonth(searchParams.get("year"), searchParams.get("month"), new Date(), minYear, maxYear);

  function moveMonth(offset: -1 | 1) {
    const next = shiftMonth(selectedMonth, offset);
    if (next.year >= minYear && next.year <= maxYear && (next.year !== selectedMonth.year || next.month !== selectedMonth.month)) {
      router.push(`${pathname}?year=${next.year}&month=${next.month}`);
    }
  }

  function goToToday() {
    const today = new Date();
    router.push(`${pathname}?year=${today.getFullYear()}&month=${today.getMonth() + 1}`);
  }

  return { ...selectedMonth, moveMonth, goToToday };
}
