"use client";
import { useCategoryList } from "@/category/hooks/useCategoryList";
import { useAccountList } from "@/account/hooks/useAccountList";
import type { TransactionType } from "../dto/TransactionType";

export function useTransactionSearchOptions(moneyBookUid: number, type: "" | TransactionType, enabled: boolean) {
  const categories = useCategoryList(moneyBookUid, type || undefined, enabled);
  const accounts = useAccountList(moneyBookUid, enabled);
  return { categories: categories.categories, accounts: accounts.accounts };
}
