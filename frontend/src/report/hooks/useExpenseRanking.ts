"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useGetExpenseRankingQuery } from "../controller/reportApi";

export function useExpenseRanking(moneyBookUid: number, periodType: "MONTH" | "YEAR", year: number, month?: number) {
  const permission = useMoneyBookPermission(moneyBookUid);
  const query = useGetExpenseRankingQuery({ moneyBookUid, periodType, year, ...(periodType === "MONTH" ? { month } : {}) }, { skip: !permission.canRead });
  return {
    permission, ranking: query.currentData, isLoading: query.isLoading,
    isFetching: query.isFetching, isError: query.isError,
    errorMessage: query.isError ? getApiErrorMessage(query.error, "지출 순위를 불러오지 못했습니다. 다시 시도해주세요.") : null,
    refetch: query.refetch,
  };
}
