"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetCalendarDayQuery } from "../controller/calendarApi";

export function useCalendarDayDetail(moneyBookUid: number, date: string | null) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetCalendarDayQuery(
    { moneyBookUid, date: date ?? "" }, { skip: date === null },
  );
  return {
    detail: currentData,
    isLoading: date !== null && (isFetching || isUninitialized) && !currentData,
    isError,
    errorMessage: isError ? getApiErrorMessage(error, "날짜별 내역을 불러오지 못했습니다.") : null,
  };
}
