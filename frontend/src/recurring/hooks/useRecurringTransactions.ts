"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetRecurringTransactionsQuery } from "../controller/recurringTransactionApi";

export function useRecurringTransactions(moneyBookUid: number, enabled = true) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetRecurringTransactionsQuery(moneyBookUid, { skip: !enabled });
  return { rules: currentData ?? [], isLoading: enabled && (isFetching || isUninitialized) && !currentData, isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "정기 수입/지출을 불러오지 못했습니다.") : null };
}
