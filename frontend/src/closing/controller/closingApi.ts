import { baseApi } from "@/common/api/baseApi";
import type { MonthClosingResponse } from "../dto/res/MonthClosingResponse";
export interface ClosingMonthKey { moneyBookUid: number; year: number; month: number }
export const closingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthClosing: builder.query<MonthClosingResponse, ClosingMonthKey>({
      query: ({ moneyBookUid, year, month }) => `money-books/${moneyBookUid}/month-closings/${year}/${month}`,
      providesTags: (_result, _error, { moneyBookUid, year, month }) => [{ type: "Closing", id: moneyBookUid }, { type: "Closing", id: `${moneyBookUid}-${year}-${month}` }],
    }),
    closeMonth: builder.mutation<MonthClosingResponse, ClosingMonthKey>({
      query: ({ moneyBookUid, year, month }) => ({ url: `money-books/${moneyBookUid}/month-closings/${year}/${month}`, method: "POST" }),
      invalidatesTags: (_result, error, { moneyBookUid, year, month }) => error ? [] : [
        { type: "Closing", id: moneyBookUid }, { type: "Closing", id: `${moneyBookUid}-${year}-${month}` }, { type: "Report", id: moneyBookUid },
      ],
    }),
    cancelMonthClosing: builder.mutation<void, ClosingMonthKey>({
      query: ({ moneyBookUid, year, month }) => ({ url: `money-books/${moneyBookUid}/month-closings/${year}/${month}`, method: "DELETE" }),
      invalidatesTags: (_result, error, { moneyBookUid, year, month }) => error ? [] : [
        { type: "Closing", id: moneyBookUid }, { type: "Closing", id: `${moneyBookUid}-${year}-${month}` }, { type: "Report", id: moneyBookUid },
      ],
    }),
  }),
});
export const { useGetMonthClosingQuery, useCloseMonthMutation, useCancelMonthClosingMutation } = closingApi;
