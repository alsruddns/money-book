"use client";

import { useCategoryList } from "@/category/hooks/useCategoryList";
import { useAccountList } from "@/account/hooks/useAccountList";
import type { TransactionType } from "../dto/TransactionType";

export function useTransactionFormOptions(moneyBookUid: number, transactionType: TransactionType) {
  const categories = useCategoryList(moneyBookUid, transactionType);
  const accounts = useAccountList(moneyBookUid);
  return {
    categories: categories.categories.filter((category) => category.transactionType === transactionType),
    accounts: accounts.accounts,
    isLoading: categories.isLoading || categories.isFetching || accounts.isLoading,
    isError: categories.isError || accounts.isError,
    errorMessage: categories.errorMessage || accounts.errorMessage,
  };
}
