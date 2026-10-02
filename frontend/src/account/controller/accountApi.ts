import { baseApi } from "@/common/api/baseApi";
import type { CreateAccountRequest } from "../dto/req/CreateAccountRequest";
import type { UpdateAccountRequest } from "../dto/req/UpdateAccountRequest";
import type { AccountResponse } from "../dto/res/AccountResponse";

interface AccountKey { moneyBookUid: number; accountUid: number }
interface CreateArg { moneyBookUid: number; request: CreateAccountRequest }
interface UpdateArg extends AccountKey { request: UpdateAccountRequest }

export const accountApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAccounts: builder.query<AccountResponse[], number>({
      query: (moneyBookUid) => `money-books/${moneyBookUid}/accounts`,
      providesTags: (_result, _error, moneyBookUid) => [{ type: "Account", id: moneyBookUid }],
    }),
    createAccount: builder.mutation<AccountResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/accounts`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Account", id: moneyBookUid }],
    }),
    updateAccount: builder.mutation<AccountResponse, UpdateArg>({
      query: ({ moneyBookUid, accountUid, request }) => ({
        url: `money-books/${moneyBookUid}/accounts/${accountUid}`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "Account", id: moneyBookUid }, { type: "Transaction", id: moneyBookUid }, { type: "Calendar", id: moneyBookUid },
      ],
    }),
    deleteAccount: builder.mutation<void, AccountKey>({
      query: ({ moneyBookUid, accountUid }) => ({
        url: `money-books/${moneyBookUid}/accounts/${accountUid}`, method: "DELETE",
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Account", id: moneyBookUid }],
    }),
  }),
});

export const { useGetAccountsQuery, useCreateAccountMutation, useUpdateAccountMutation, useDeleteAccountMutation } = accountApi;
