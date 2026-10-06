import { baseApi } from "@/common/api/baseApi";
import type { MoneyBookActivityPageResponse } from "../dto/res/MoneyBookActivityPageResponse";
export interface MoneyBookActivityQuery { moneyBookUid: number; startDate?: string; endDate?: string; actorUserUid?: number; activityType?: string; targetType?: string; page: number; size: number }
export const activityApi = baseApi.injectEndpoints({ endpoints: (builder) => ({
  getMoneyBookActivities: builder.query<MoneyBookActivityPageResponse, MoneyBookActivityQuery>({
    query: ({ moneyBookUid, ...params }) => ({ url: `money-books/${moneyBookUid}/activities`, params }),
    providesTags: (_result, _error, { moneyBookUid }) => [{ type: "MoneyBookActivity", id: moneyBookUid }],
  }),
}) });
export const { useGetMoneyBookActivitiesQuery } = activityApi;
