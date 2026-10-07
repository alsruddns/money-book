import { baseApi } from "@/common/api/baseApi";
import type { DashboardResponse } from "../dto/res/DashboardResponse";

export interface DashboardQuery { moneyBookUid: number; year: number; month: number }

export const dashboardApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getDashboard: builder.query<DashboardResponse, DashboardQuery>({
      query: ({ moneyBookUid, year, month }) => ({ url: `money-books/${moneyBookUid}/dashboard`, params: { year, month } }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Dashboard", id: moneyBookUid }],
    }),
  }),
});

export const { useGetDashboardQuery } = dashboardApi;
