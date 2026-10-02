"use client";

import { useState } from "react";
import { formatMoney } from "@/common/format/money";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import MonthSelector from "@/transaction/components/MonthSelector";
import { buildCalendarGrid, dayNumber, getWeekdayLabels } from "../calendarGrid";
import { useMonthlyCalendar } from "../hooks/useMonthlyCalendar";
import { useMoneyBookSetting } from "@/settings/hooks/useMoneyBookSetting";
import type { CalendarDayResponse } from "../dto/res/MonthlyCalendarResponse";
import CalendarDayDetailDialog from "./CalendarDayDetailDialog";

export default function CalendarView({ moneyBookUid }: { moneyBookUid: number }) {
  const [selectedDay, setSelectedDay] = useState<CalendarDayResponse | null>(null);
  const { canRead } = useMoneyBookPermission(moneyBookUid);
  const { year, month, moveMonth, goToToday } = useMonthNavigation();
  const { setting } = useMoneyBookSetting(moneyBookUid, canRead);
  const weekStartDay = setting?.weekStartDay ?? "SUNDAY";
  const { calendar, isLoading, isFetching, isError, errorMessage } = useMonthlyCalendar(moneyBookUid, year, month, canRead);
  const cells = buildCalendarGrid(calendar?.days ?? [], weekStartDay);
  function changeMonth(offset: -1 | 1) { setSelectedDay(null); moveMonth(offset); }

  return <div className="space-y-5">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><h1 className="text-2xl font-semibold">캘린더</h1><p className="mt-1 text-sm text-zinc-600">날짜별 거래와 이체를 확인합니다.</p></div>
      <div className="flex flex-wrap items-center gap-2">
        <MonthSelector year={year} month={month} onPrevious={() => changeMonth(-1)} onNext={() => changeMonth(1)} />
        <button type="button" onClick={() => { setSelectedDay(null); goToToday(); }} className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm">오늘</button>
      </div>
    </div>
    {!canRead ? <p role="alert">캘린더 조회 권한이 없습니다.</p> :
      isLoading ? <p role="status" className="rounded-xl bg-white p-6">달력을 불러오는 중...</p> :
      isError ? <p role="alert" className="rounded-xl bg-white p-6 text-red-600">{errorMessage}</p> :
      calendar && <div className="min-w-0 overflow-hidden rounded-xl border border-zinc-200 bg-white">
        {isFetching && <p role="status" className="border-b border-zinc-200 px-3 py-2 text-sm">달력을 새로고침하는 중...</p>}
        <div className="grid grid-cols-7 border-b border-zinc-200 bg-zinc-50 text-center text-xs font-semibold sm:text-sm">
          {getWeekdayLabels(weekStartDay).map((label) => <div key={label} className={`py-2 ${label === "일" ? "text-red-700" : label === "토" ? "text-blue-700" : ""}`}>{label}</div>)}
        </div>
        <div className="grid grid-cols-7">
          {cells.map((day, index) => day ?
            <button key={day.date} type="button" onClick={() => setSelectedDay(day)} aria-label={`${day.date} 상세 보기`}
              className="min-h-24 min-w-0 border-b border-r border-zinc-100 p-1 text-left hover:bg-blue-50 focus-visible:outline-2 focus-visible:outline-blue-600 sm:min-h-32 sm:p-2">
              <span className={`block text-sm font-semibold ${day.holiday || day.dayOfWeek === "SUNDAY" ? "text-red-700" : day.weekend ? "text-blue-700" : ""}`}>
                {dayNumber(day.date)}{day.holiday && <span className="ml-1 text-[10px]">공휴일</span>}
              </span>
              {day.holidayName && <span className="block truncate text-[10px] text-red-700 sm:text-xs" title={day.holidayName}>{day.holidayName}</span>}
              <span className="mt-1 block space-y-0.5 text-[10px] leading-tight sm:text-xs">
                {Number(day.incomeAmount) > 0 && <span className="block truncate text-blue-700" title={`수입 ${formatMoney(day.incomeAmount)}`}>수입 +{formatMoney(day.incomeAmount)}</span>}
                {Number(day.expenseAmount) > 0 && <span className="block truncate text-red-700" title={`지출 ${formatMoney(day.expenseAmount)}`}>지출 −{formatMoney(day.expenseAmount)}</span>}
                {day.transferCount > 0 && <span className="block truncate text-zinc-600" title={`이체 ${formatMoney(day.transferOutAmount)}`}>이체 {day.transferCount}건</span>}
              </span>
            </button> : <div key={`empty-${index}`} aria-hidden="true" className="border-b border-r border-zinc-100 bg-zinc-50" />)}
        </div>
      </div>}
    {selectedDay && <CalendarDayDetailDialog moneyBookUid={moneyBookUid} day={selectedDay} onClose={() => setSelectedDay(null)} />}
  </div>;
}
