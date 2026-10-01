import { baseApi } from "@/common/api/baseApi";
import type { CreateTransactionRequest } from "../dto/req/CreateTransactionRequest";
import type { UpdateTransactionRequest } from "../dto/req/UpdateTransactionRequest";
import type { TransactionResponse } from "../dto/res/TransactionResponse";

export interface MonthQuery { moneyBookUid: number; year: number; month: number }
interface TransactionKey { moneyBookUid: number; transactionUid: number }
interface CreateArg { moneyBookUid: number; request: CreateTransactionRequest }
interface UpdateArg extends TransactionKey { request: UpdateTransactionRequest }

export const transactionApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlyTransactions: builder.query<TransactionResponse[], MonthQuery>({
      query: ({ moneyBookUid, year, month }) => ({
        url: `money-books/${moneyBookUid}/transactions`, params: { year, month },
      }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Transaction", id: moneyBookUid }],
    }),
    getTransaction: builder.query<TransactionResponse, TransactionKey>({
      query: ({ moneyBookUid, transactionUid }) => `money-books/${moneyBookUid}/transactions/${transactionUid}`,
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Transaction", id: moneyBookUid }],
    }),
    createTransaction: builder.mutation<TransactionResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/transactions`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Transaction", id: moneyBookUid }],
    }),
    updateTransaction: builder.mutation<TransactionResponse, UpdateArg>({
      query: ({ moneyBookUid, transactionUid, request }) => ({
        url: `money-books/${moneyBookUid}/transactions/${transactionUid}`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Transaction", id: moneyBookUid }],
    }),
    deleteTransaction: builder.mutation<void, TransactionKey>({
      query: ({ moneyBookUid, transactionUid }) => ({ url: `money-books/${moneyBookUid}/transactions/${transactionUid}`, method: "DELETE" }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Transaction", id: moneyBookUid }],
    }),
  }),
});

export const {
  useGetMonthlyTransactionsQuery, useGetTransactionQuery, useCreateTransactionMutation,
  useUpdateTransactionMutation, useDeleteTransactionMutation,
} = transactionApi;
