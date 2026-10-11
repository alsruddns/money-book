"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import { useGetCategoryTreeQuery } from "../controller/categoryApi";

export function useCategoryTree(moneyBookUid: number, transactionType?: TransactionType, enabled = true) {
  const { currentData, isLoading, isFetching, isError, error } = useGetCategoryTreeQuery(
    { moneyBookUid, transactionType }, { skip: !enabled },
  );
  return {
    categories: currentData ?? [], isLoading, isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "Could not load categories.") : null,
  };
}
