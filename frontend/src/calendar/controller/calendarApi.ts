import { baseApi } from "@/common/api/baseApi";
import type { MonthlyCalendarResponse } from "../dto/res/MonthlyCalendarResponse";
import type { DailyCalendarResponse } from "../dto/res/DailyCalendarResponse";

export interface CalendarMonthKey { moneyBookUid: number; year: number; month: number }
export interface CalendarDayKey { moneyBookUid: number; date: string }

export const calendarApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlyCalendar: builder.query<MonthlyCalendarResponse, CalendarMonthKey>({
      query: ({ moneyBookUid, year, month }) => ({
        url: `money-books/${moneyBookUid}/calendar`, params: { year, month },
      }),
      providesTags: (_result, _error, { moneyBookUid, year, month }) => [
        { type: "Calendar", id: moneyBookUid }, { type: "Calendar", id: `${moneyBookUid}-${year}-${month}` },
      ],
    }),
    getCalendarDay: builder.query<DailyCalendarResponse, CalendarDayKey>({
      query: ({ moneyBookUid, date }) => `money-books/${moneyBookUid}/calendar/${date}`,
      providesTags: (_result, _error, { moneyBookUid, date }) => [
        { type: "Calendar", id: moneyBookUid }, { type: "Calendar", id: `${moneyBookUid}-${date}` },
      ],
    }),
  }),
});

export const { useGetMonthlyCalendarQuery, useGetCalendarDayQuery } = calendarApi;
