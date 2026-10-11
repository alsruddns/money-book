"use client";

import { useCategoryTree } from "@/category/hooks/useCategoryTree";
import { useAccountList } from "@/account/hooks/useAccountList";
import type { TransactionType } from "../dto/TransactionType";

export function useTransactionFormOptions(moneyBookUid: number, transactionType: TransactionType) {
  const categories = useCategoryTree(moneyBookUid, transactionType);
  const accounts = useAccountList(moneyBookUid);
  return {
    categories: categories.categories,
    accounts: accounts.accounts,
    isLoading: categories.isLoading || categories.isFetching || accounts.isLoading,
    isError: categories.isError || accounts.isError,
    errorMessage: categories.errorMessage || accounts.errorMessage,
  };
}
