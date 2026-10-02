import { baseApi } from "@/common/api/baseApi";
import type { CreateTransferRequest } from "../dto/req/CreateTransferRequest";
import type { UpdateTransferRequest } from "../dto/req/UpdateTransferRequest";
import type { TransferResponse } from "../dto/res/TransferResponse";

interface MonthKey { moneyBookUid: number; year: number; month: number }
interface TransferKey { moneyBookUid: number; transferUid: number }
interface CreateArg { moneyBookUid: number; request: CreateTransferRequest }
interface UpdateArg extends TransferKey { request: UpdateTransferRequest }
const affected = (moneyBookUid: number) => [
  { type: "Transfer" as const, id: moneyBookUid }, { type: "Calendar" as const, id: moneyBookUid },
];

export const transferApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlyTransfers: builder.query<TransferResponse[], MonthKey>({
      query: ({ moneyBookUid, year, month }) => ({ url: `money-books/${moneyBookUid}/transfers`, params: { year, month } }),
      providesTags: (_result, _error, { moneyBookUid, year, month }) => [
        { type: "Transfer", id: moneyBookUid }, { type: "Transfer", id: `${moneyBookUid}-${year}-${month}` },
      ],
    }),
    getTransfer: builder.query<TransferResponse, TransferKey>({
      query: ({ moneyBookUid, transferUid }) => `money-books/${moneyBookUid}/transfers/${transferUid}`,
      providesTags: (_result, _error, { moneyBookUid, transferUid }) => [
        { type: "Transfer", id: moneyBookUid }, { type: "Transfer", id: `${moneyBookUid}-${transferUid}` },
      ],
    }),
    createTransfer: builder.mutation<TransferResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/transfers`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : affected(moneyBookUid),
    }),
    updateTransfer: builder.mutation<TransferResponse, UpdateArg>({
      query: ({ moneyBookUid, transferUid, request }) => ({ url: `money-books/${moneyBookUid}/transfers/${transferUid}`, method: "PATCH", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : affected(moneyBookUid),
    }),
    deleteTransfer: builder.mutation<void, TransferKey>({
      query: ({ moneyBookUid, transferUid }) => ({ url: `money-books/${moneyBookUid}/transfers/${transferUid}`, method: "DELETE" }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : affected(moneyBookUid),
    }),
  }),
});

export const {
  useGetMonthlyTransfersQuery, useGetTransferQuery, useCreateTransferMutation,
  useUpdateTransferMutation, useDeleteTransferMutation,
} = transferApi;
