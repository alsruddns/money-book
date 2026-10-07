"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetTransferQuery } from "../controller/transferApi";

export function useTransferDetail(moneyBookUid: number, transferUid: number) {
  const { currentData, isFetching, isError, error } = useGetTransferQuery({ moneyBookUid, transferUid });
  return { transfer: currentData, isLoading: isFetching && !currentData, isError,
    errorMessage: isError ? getApiErrorMessage(error, "이체 상세를 불러오지 못했습니다.") : null };
}
