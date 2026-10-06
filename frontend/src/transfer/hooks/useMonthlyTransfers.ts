"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMonthlyTransfersQuery } from "../controller/transferApi";

export function useMonthlyTransfers(moneyBookUid: number, year: number, month: number, enabled = true) {
  const { currentData, isFetching, isUninitialized, isError, error } = useGetMonthlyTransfersQuery(
    { moneyBookUid, year, month }, { skip: !enabled },
  );
  return {
    transfers: currentData ?? [], isLoading: enabled && (isFetching || isUninitialized) && !currentData,
    isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "이체 내역을 불러오지 못했습니다.") : null,
  };
}
