import { baseApi } from "@/common/api/baseApi";
import type { CreateAccountRequest } from "../dto/req/CreateAccountRequest";
import type { UpdateAccountRequest } from "../dto/req/UpdateAccountRequest";
import type { AccountResponse } from "../dto/res/AccountResponse";
import type { AccountProfileUpdateReqDto } from "../dto/req/AccountProfileUpdateReqDto";
import type { AccountPasswordUpdateReqDto } from "../dto/req/AccountPasswordUpdateReqDto";
import type { AccountMeResDto } from "../dto/res/AccountMeResDto";
import type { AccountWithdrawalRequest } from "../dto/req/AccountWithdrawalRequest";
import type { RefreshSessionResponse } from "../dto/res/RefreshSessionResponse";

interface AccountKey { moneyBookUid: number; accountUid: number }
interface CreateArg { moneyBookUid: number; request: CreateAccountRequest }
interface UpdateArg extends AccountKey { request: UpdateAccountRequest }

export const accountApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAccountMe: builder.query<AccountMeResDto, void>({
      query: () => "account/me",
      providesTags: ["AccountMe"],
    }),
    updateAccountProfile: builder.mutation<AccountMeResDto, AccountProfileUpdateReqDto>({
      query: (body) => ({ url: "account/profile", method: "PATCH", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountMe", "AuthMe"],
    }),
    updateAccountPassword: builder.mutation<AccountMeResDto, AccountPasswordUpdateReqDto>({
      query: (body) => ({ url: "account/password", method: "PATCH", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountMe"],
    }),
    withdrawAccount: builder.mutation<void, AccountWithdrawalRequest>({
      query: (body) => ({ url: "account", method: "DELETE", body }),
    }),
    getAccountSessions: builder.query<RefreshSessionResponse[], void>({
      query: () => "account/sessions",
      providesTags: ["AccountSessions"],
    }),
    revokeAccountSession: builder.mutation<void, number>({
      query: (sessionUid) => ({ url: `account/sessions/${sessionUid}`, method: "DELETE" }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountSessions"],
    }),
    logoutAllAccountSessions: builder.mutation<void, void>({
      query: () => ({ url: "account/sessions/logout-all", method: "POST" }),
    }),
    getAccounts: builder.query<AccountResponse[], number>({
      query: (moneyBookUid) => `money-books/${moneyBookUid}/accounts`,
      providesTags: (_result, _error, moneyBookUid) => [{ type: "Account", id: moneyBookUid }],
    }),
    createAccount: builder.mutation<AccountResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/accounts`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Account", id: moneyBookUid }, { type: "Report", id: moneyBookUid }, { type: "Dashboard", id: moneyBookUid }, { type: "MoneyBookActivity", id: moneyBookUid }],
    }),
    updateAccount: builder.mutation<AccountResponse, UpdateArg>({
      query: ({ moneyBookUid, accountUid, request }) => ({
        url: `money-books/${moneyBookUid}/accounts/${accountUid}`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "Account", id: moneyBookUid }, { type: "Transaction", id: moneyBookUid }, { type: "Calendar", id: moneyBookUid },
        { type: "Transfer", id: moneyBookUid }, { type: "Recurring", id: moneyBookUid }, { type: "Report", id: moneyBookUid }, { type: "Dashboard", id: moneyBookUid }, { type: "MoneyBookActivity", id: moneyBookUid },
      ],
    }),
    deleteAccount: builder.mutation<void, AccountKey>({
      query: ({ moneyBookUid, accountUid }) => ({
        url: `money-books/${moneyBookUid}/accounts/${accountUid}`, method: "DELETE",
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Account", id: moneyBookUid }, { type: "Report", id: moneyBookUid }, { type: "Dashboard", id: moneyBookUid }, { type: "MoneyBookActivity", id: moneyBookUid }],
    }),
  }),
});

export const {
  useGetAccountMeQuery,
  useUpdateAccountProfileMutation,
  useUpdateAccountPasswordMutation,
  useWithdrawAccountMutation,
  useGetAccountSessionsQuery,
  useRevokeAccountSessionMutation,
  useLogoutAllAccountSessionsMutation,
  useGetAccountsQuery,
  useCreateAccountMutation,
  useUpdateAccountMutation,
  useDeleteAccountMutation,
} = accountApi;
