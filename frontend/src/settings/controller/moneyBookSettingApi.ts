import { baseApi } from "@/common/api/baseApi";
import type { UpdateMoneyBookSettingRequest } from "../dto/req/UpdateMoneyBookSettingRequest";
import type { MoneyBookSettingResponse } from "../dto/res/MoneyBookSettingResponse";
interface UpdateArg { moneyBookUid: number; request: UpdateMoneyBookSettingRequest }
export const moneyBookSettingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMoneyBookSetting: builder.query<MoneyBookSettingResponse, number>({
      query: (moneyBookUid) => `money-books/${moneyBookUid}/settings`,
      providesTags: (_result, _error, moneyBookUid) => [{ type: "MoneyBookSetting", id: moneyBookUid }],
    }),
    updateMoneyBookSetting: builder.mutation<MoneyBookSettingResponse, UpdateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/settings`, method: "PUT", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "MoneyBookSetting", id: moneyBookUid }, { type: "Calendar", id: moneyBookUid },
      ],
    }),
  }),
});
export const { useGetMoneyBookSettingQuery, useUpdateMoneyBookSettingMutation } = moneyBookSettingApi;
