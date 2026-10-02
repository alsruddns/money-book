import { baseApi } from "@/common/api/baseApi";
import type { SaveBudgetRequest } from "../dto/req/SaveBudgetRequest";
import type { MonthlyBudgetResponse } from "../dto/res/MonthlyBudgetResponse";

export interface BudgetMonthKey { moneyBookUid: number; year: number; month: number }
interface SaveBudgetArg extends BudgetMonthKey { request: SaveBudgetRequest }

export const budgetApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlyBudget: builder.query<MonthlyBudgetResponse, BudgetMonthKey>({
      query: ({ moneyBookUid, year, month }) => `money-books/${moneyBookUid}/budgets/${year}/${month}`,
      providesTags: (_result, _error, { moneyBookUid, year, month }) => [
        { type: "Budget", id: moneyBookUid }, { type: "Budget", id: `${moneyBookUid}-${year}-${month}` },
      ],
    }),
    saveMonthlyBudget: builder.mutation<MonthlyBudgetResponse, SaveBudgetArg>({
      query: ({ moneyBookUid, year, month, request }) => ({
        url: `money-books/${moneyBookUid}/budgets/${year}/${month}`, method: "PUT", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid, year, month }) => error ? [] : [
        { type: "Budget", id: `${moneyBookUid}-${year}-${month}` }, { type: "Report", id: moneyBookUid }, { type: "MoneyBookActivity", id: moneyBookUid },
      ],
    }),
  }),
});

export const { useGetMonthlyBudgetQuery, useSaveMonthlyBudgetMutation } = budgetApi;
