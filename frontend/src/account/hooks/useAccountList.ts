"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetAccountsQuery } from "../controller/accountApi";

export function useAccountList(moneyBookUid: number) {
  const { currentData, isLoading, isError, error } = useGetAccountsQuery(moneyBookUid);
  return {
    accounts: currentData ?? [], isLoading, isError,
    errorMessage: isError ? getApiErrorMessage(error, "계좌와 결제수단을 불러오지 못했습니다.") : null,
  };
}
