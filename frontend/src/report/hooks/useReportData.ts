"use client";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useGetAccountStatisticsQuery, useGetCategoryStatisticsQuery, useGetMonthlyReportQuery, useGetYearlyReportQuery } from "../controller/reportApi";

export function useReportData(moneyBookUid: number, year: number, month: number, yearly: boolean) {
  const permission = useMoneyBookPermission(moneyBookUid);
  const enabled = permission.canRead;
  const monthly = useGetMonthlyReportQuery({ moneyBookUid, year, month }, { skip: !enabled || yearly });
  const annual = useGetYearlyReportQuery({ moneyBookUid, year }, { skip: !enabled || !yearly });
  const startDate = `${year}-${String(month).padStart(2, "0")}-01`;
  const endDate = `${year}-${String(month).padStart(2, "0")}-${new Date(year, month, 0).getDate()}`;
  const expenseCategories = useGetCategoryStatisticsQuery({ moneyBookUid, startDate, endDate, transactionType: "EXPENSE" }, { skip: !enabled || yearly });
  const incomeCategories = useGetCategoryStatisticsQuery({ moneyBookUid, startDate, endDate, transactionType: "INCOME" }, { skip: !enabled || yearly });
  const accounts = useGetAccountStatisticsQuery({ moneyBookUid, startDate, endDate }, { skip: !enabled || yearly });
  const queries = yearly ? [annual] : [monthly, expenseCategories, incomeCategories, accounts];
  const failed = queries.find((query) => query.isError);
  return { permission, monthly: monthly.currentData, annual: annual.currentData,
    expenseCategories: expenseCategories.currentData ?? [], incomeCategories: incomeCategories.currentData ?? [], accounts: accounts.currentData ?? [],
    isLoading: queries.some((query) => query.isLoading || query.isFetching && !query.currentData),
    isError: Boolean(failed), errorMessage: failed?.isError ? getApiErrorMessage(failed.error, "리포트를 불러오지 못했습니다.") : null };
}
