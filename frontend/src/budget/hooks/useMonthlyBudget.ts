"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMonthlyBudgetQuery } from "../controller/budgetApi";

export function useMonthlyBudget(moneyBookUid: number, year: number, month: number, enabled = true) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetMonthlyBudgetQuery(
    { moneyBookUid, year, month }, { skip: !enabled },
  );
  return {
    budget: currentData,
    isLoading: enabled && (isFetching || isUninitialized) && !currentData,
    isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "예산을 불러오지 못했습니다.") : null,
  };
}
