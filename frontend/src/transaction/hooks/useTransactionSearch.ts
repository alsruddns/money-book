"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useSearchTransactionsQuery } from "../controller/transactionApi";
import { buildTransactionSearchParams, filtersToSearchQuery, parseTransactionFilters, type TransactionFilters } from "../search";

export function useTransactionSearch(moneyBookUid: number, year: number, month: number, enabled: boolean) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const filters = parseTransactionFilters(new URLSearchParams(params.toString()), year, month);
  const result = useSearchTransactionsQuery(filtersToSearchQuery(moneyBookUid, filters), { skip: !enabled });
  function update(next: TransactionFilters, resetPage = false) {
    const search = buildTransactionSearchParams(new URLSearchParams(params.toString()), next, resetPage);
    router.push(`${pathname}?${search.toString()}`);
  }
  function showMonthly() {
    const search = new URLSearchParams(params.toString());
    search.delete("search"); search.delete("page");
    router.push(`${pathname}?${search.toString()}`);
  }
  return { filters, update, showMonthly, result: result.currentData, isLoading: result.isFetching && !result.currentData,
    isError: result.isError, errorMessage: result.isError ? getApiErrorMessage(result.error, "거래 검색에 실패했습니다.") : null };
}
