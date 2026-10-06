"use client";

import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetDashboardQuery } from "../controller/dashboardApi";

export function useMonthlyDashboard(moneyBookUid: number) {
  const { year, month, moveMonth, goToToday } = useMonthNavigation(2, 9998);
  const permission = useMoneyBookPermission(moneyBookUid);
  const query = useGetDashboardQuery({ moneyBookUid, year, month }, { skip: !permission.canRead });
  return {
    year, month, moveMonth, goToToday, permission, dashboard: query.currentData,
    isLoading: query.isLoading, isFetching: query.isFetching, isError: query.isError,
    errorMessage: query.isError ? getApiErrorMessage(query.error, "대시보드 데이터를 불러오지 못했습니다. 다시 시도해주세요.") : null,
    refetch: query.refetch,
  };
}
