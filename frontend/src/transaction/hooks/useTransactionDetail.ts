"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetTransactionQuery } from "../controller/transactionApi";

export function useTransactionDetail(moneyBookUid: number, transactionUid: number | null) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetTransactionQuery(
    { moneyBookUid, transactionUid: transactionUid ?? 0 }, { skip: transactionUid === null },
  );
  return {
    transaction: currentData ?? null, isLoading: (isFetching || isUninitialized) && !currentData, isError,
    errorMessage: isError ? getApiErrorMessage(error, "거래 상세를 불러오지 못했습니다.") : null,
  };
}
