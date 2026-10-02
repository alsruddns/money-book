"use client";

import { useMemo } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMonthlyCalendarQuery } from "../controller/calendarApi";

export function useMonthlyCalendar(moneyBookUid: number, year: number, month: number, enabled = true) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetMonthlyCalendarQuery(
    { moneyBookUid, year, month }, { skip: !enabled },
  );
  const totals = useMemo(() => (currentData?.days ?? []).reduce((sum, day) => ({
    income: sum.income + Number(day.incomeAmount),
    expense: sum.expense + Number(day.expenseAmount),
    transferIn: sum.transferIn + Number(day.transferInAmount),
    transferOut: sum.transferOut + Number(day.transferOutAmount),
  }), { income: 0, expense: 0, transferIn: 0, transferOut: 0 }), [currentData]);
  return {
    calendar: currentData, totals,
    isLoading: enabled && (isFetching || isUninitialized) && !currentData,
    isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "달력을 불러오지 못했습니다.") : null,
  };
}
