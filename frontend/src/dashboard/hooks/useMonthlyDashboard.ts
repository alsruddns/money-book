"use client";

import { useSyncExternalStore } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthlyCalendar } from "@/calendar/hooks/useMonthlyCalendar";
import { useMonthlyBudget } from "@/budget/hooks/useMonthlyBudget";

function currentBrowserMonth(): string {
  const today = new Date();
  return `${today.getFullYear()}-${today.getMonth() + 1}`;
}

function serverMonth(): string { return ""; }
function subscribe(): () => void { return () => {}; }

export function useMonthlyDashboard(moneyBookUid: number) {
  const currentMonth = useSyncExternalStore(subscribe, currentBrowserMonth, serverMonth);
  const [yearText, monthText] = currentMonth.split("-");
  const year = Number(yearText) || 1;
  const month = Number(monthText) || 1;
  const permission = useMoneyBookPermission(moneyBookUid);
  const enabled = currentMonth !== "" && permission.canRead;
  const calendar = useMonthlyCalendar(moneyBookUid, year, month, enabled);
  const budget = useMonthlyBudget(moneyBookUid, year, month, enabled);
  const activeDays = (calendar.calendar?.days ?? []).filter((day) => day.transactionCount > 0 || day.transferCount > 0).slice(-5).reverse();
  return { year, month, isMonthReady: currentMonth !== "", permission, calendar, budget, activeDays };
}
