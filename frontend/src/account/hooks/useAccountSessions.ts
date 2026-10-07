"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetAccountSessionsQuery } from "../controller/accountApi";

export function useAccountSessions() {
  const query = useGetAccountSessionsQuery();
  const sessions = [...(query.currentData ?? [])].sort((left, right) => Number(right.current) - Number(left.current));
  return {
    sessions,
    isLoading: query.isLoading || query.isUninitialized,
    isFetching: query.isFetching,
    isError: query.isError,
    errorMessage: query.isError ? getApiErrorMessage(query.error, "로그인 세션을 불러오지 못했습니다.") : null,
    retry: query.refetch,
  };
}
