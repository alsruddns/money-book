"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import { useGetCategoriesQuery } from "../controller/categoryApi";

export function useCategoryList(moneyBookUid: number, transactionType?: TransactionType, enabled = true) {
  const { currentData, isLoading, isFetching, isError, error } = useGetCategoriesQuery({ moneyBookUid, transactionType }, { skip: !enabled });
  return {
    categories: currentData ?? [], isLoading, isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "카테고리를 불러오지 못했습니다.") : null,
  };
}
