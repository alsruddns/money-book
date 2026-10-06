import { baseApi } from "@/common/api/baseApi";
import type { MonthlyReportResponse } from "../dto/res/MonthlyReportResponse";
import type { YearlyReportResponse } from "../dto/res/YearlyReportResponse";
import type { CategoryStatisticsResponse } from "../dto/res/CategoryStatisticsResponse";
import type { AccountStatisticsResponse } from "../dto/res/AccountStatisticsResponse";

export interface ReportMonthQuery { moneyBookUid: number; year: number; month: number }
export interface ReportYearQuery { moneyBookUid: number; year: number }
export interface ReportPeriodQuery { moneyBookUid: number; startDate: string; endDate: string }
interface CategoryQuery extends ReportPeriodQuery { transactionType: "INCOME" | "EXPENSE" }
export interface ExpenseRankingQuery { moneyBookUid: number; periodType: "MONTH" | "YEAR"; year: number; month?: number }
export interface ExpenseRankingResponse {
  rank: number; transactionUid: number; transactionDate: string; categoryUid: number;
  categoryName: string; accountUid: number; accountName: string; memo: string | null; amount: number;
}
export const reportApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlyReport: builder.query<MonthlyReportResponse, ReportMonthQuery>({
      query: ({ moneyBookUid, year, month }) => ({ url: `money-books/${moneyBookUid}/reports/monthly`, params: { year, month } }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Report", id: moneyBookUid }],
    }),
    getYearlyReport: builder.query<YearlyReportResponse, ReportYearQuery>({
      query: ({ moneyBookUid, year }) => ({ url: `money-books/${moneyBookUid}/reports/yearly`, params: { year } }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Report", id: moneyBookUid }],
    }),
    getCategoryStatistics: builder.query<CategoryStatisticsResponse[], CategoryQuery>({
      query: ({ moneyBookUid, ...params }) => ({ url: `money-books/${moneyBookUid}/reports/categories`, params }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Report", id: moneyBookUid }],
    }),
    getAccountStatistics: builder.query<AccountStatisticsResponse[], ReportPeriodQuery>({
      query: ({ moneyBookUid, ...params }) => ({ url: `money-books/${moneyBookUid}/reports/accounts`, params }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Report", id: moneyBookUid }],
    }),
    getExpenseRanking: builder.query<ExpenseRankingResponse[], ExpenseRankingQuery>({
      query: ({ moneyBookUid, ...params }) => ({ url: `money-books/${moneyBookUid}/reports/expense-ranking`, params }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Report", id: moneyBookUid }],
    }),
  }),
});
export const { useGetMonthlyReportQuery, useGetYearlyReportQuery, useGetCategoryStatisticsQuery, useGetAccountStatisticsQuery, useGetExpenseRankingQuery } = reportApi;
