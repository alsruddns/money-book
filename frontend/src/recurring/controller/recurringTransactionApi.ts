import { baseApi } from "@/common/api/baseApi";
import type { CreateRecurringTransactionRequest } from "../dto/req/CreateRecurringTransactionRequest";
import type { UpdateRecurringTransactionRequest } from "../dto/req/UpdateRecurringTransactionRequest";
import type { UpdateRecurringTransactionActiveRequest } from "../dto/req/UpdateRecurringTransactionActiveRequest";
import type { GenerateRecurringTransactionRequest } from "../dto/req/GenerateRecurringTransactionRequest";
import type { RecurringTransactionResponse } from "../dto/res/RecurringTransactionResponse";
import type { GenerateRecurringTransactionResponse } from "../dto/res/GenerateRecurringTransactionResponse";

interface RuleKey { moneyBookUid: number; recurringTransactionUid: number }
interface CreateArg { moneyBookUid: number; request: CreateRecurringTransactionRequest }
interface UpdateArg extends RuleKey { request: UpdateRecurringTransactionRequest }
interface ActiveArg extends RuleKey { request: UpdateRecurringTransactionActiveRequest }
interface GenerateArg { moneyBookUid: number; request: GenerateRecurringTransactionRequest }
const recurringTag = (moneyBookUid: number) => [{ type: "Recurring" as const, id: moneyBookUid }];

export const recurringTransactionApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getRecurringTransactions: builder.query<RecurringTransactionResponse[], number>({
      query: (moneyBookUid) => `money-books/${moneyBookUid}/recurring-transactions`,
      providesTags: (_result, _error, moneyBookUid) => recurringTag(moneyBookUid),
    }),
    createRecurringTransaction: builder.mutation<RecurringTransactionResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/recurring-transactions`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : recurringTag(moneyBookUid),
    }),
    updateRecurringTransaction: builder.mutation<RecurringTransactionResponse, UpdateArg>({
      query: ({ moneyBookUid, recurringTransactionUid, request }) => ({
        url: `money-books/${moneyBookUid}/recurring-transactions/${recurringTransactionUid}`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : recurringTag(moneyBookUid),
    }),
    changeRecurringTransactionActive: builder.mutation<RecurringTransactionResponse, ActiveArg>({
      query: ({ moneyBookUid, recurringTransactionUid, request }) => ({
        url: `money-books/${moneyBookUid}/recurring-transactions/${recurringTransactionUid}/active`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : recurringTag(moneyBookUid),
    }),
    deleteRecurringTransaction: builder.mutation<void, RuleKey>({
      query: ({ moneyBookUid, recurringTransactionUid }) => ({
        url: `money-books/${moneyBookUid}/recurring-transactions/${recurringTransactionUid}`, method: "DELETE",
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : recurringTag(moneyBookUid),
    }),
    generateRecurringTransactions: builder.mutation<GenerateRecurringTransactionResponse, GenerateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/recurring-transactions/generate`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        ...recurringTag(moneyBookUid), { type: "Transaction", id: moneyBookUid },
        { type: "Calendar", id: moneyBookUid }, { type: "Budget", id: moneyBookUid },
      ],
    }),
  }),
});

export const {
  useGetRecurringTransactionsQuery, useCreateRecurringTransactionMutation, useUpdateRecurringTransactionMutation,
  useChangeRecurringTransactionActiveMutation, useDeleteRecurringTransactionMutation,
  useGenerateRecurringTransactionsMutation,
} = recurringTransactionApi;
