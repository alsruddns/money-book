"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMonthlyTransactionsQuery } from "../controller/transactionApi";

export function useMonthlyTransactions(moneyBookUid: number, year: number, month: number) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetMonthlyTransactionsQuery({ moneyBookUid, year, month });
  return {
    transactions: currentData ?? [], isLoading: (isFetching || isUninitialized) && !currentData, isError,
    errorMessage: isError ? getApiErrorMessage(error, "거래 내역을 불러오지 못했습니다.") : null,
  };
}
