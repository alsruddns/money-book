"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMoneyBooksQuery } from "../controller/moneyBookApi";

export function useMoneyBookList() {
  const { data, isLoading, isError, error } = useGetMoneyBooksQuery();
  return {
    moneyBooks: data ?? [],
    isLoading,
    isError,
    errorMessage: isError ? getApiErrorMessage(error, "가계부 목록을 불러오지 못했습니다.") : null,
  };
}
