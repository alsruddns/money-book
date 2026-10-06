"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetAccountMeQuery } from "../controller/accountApi";

export function useAccountMe() {
  const query = useGetAccountMeQuery();
  return {
    account: query.currentData ?? null,
    isLoading: query.isLoading || query.isUninitialized,
    isError: query.isError,
    errorMessage: query.isError ? getApiErrorMessage(query.error, "계정 정보를 불러오지 못했습니다.") : null,
    retry: query.refetch,
  };
}
