"use client";

import { useEffect } from "react";
import DialogShell from "@/common/components/DialogShell";
import { formatMoney } from "@/common/format/money";
import { useCalendarDayDetail } from "../hooks/useCalendarDayDetail";
import type { CalendarDayResponse } from "../dto/res/MonthlyCalendarResponse";

const weekdayName: Record<CalendarDayResponse["dayOfWeek"], string> = {
  SUNDAY: "일요일", MONDAY: "월요일", TUESDAY: "화요일", WEDNESDAY: "수요일",
  THURSDAY: "목요일", FRIDAY: "금요일", SATURDAY: "토요일",
};

export default function CalendarDayDetailDialog({ moneyBookUid, day, onClose }: {
  moneyBookUid: number; day: CalendarDayResponse; onClose: () => void;
}) {
  const { detail, isLoading, isError, errorMessage } = useCalendarDayDetail(moneyBookUid, day.date);
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return <DialogShell title={`${day.date} ${weekdayName[day.dayOfWeek]}`} onClose={onClose}>
    {(detail?.holiday || day.holiday) && <p className="mb-4 text-sm font-medium text-red-700">공휴일 · {detail?.holidayName ?? day.holidayName}</p>}
    {isLoading ? <p role="status">날짜별 내역을 불러오는 중...</p> :
      isError ? <p role="alert" className="text-red-600">{errorMessage}</p> : detail && <div className="space-y-5">
        {detail.transactions.length === 0 && detail.transfers.length === 0 ?
          <p className="text-sm text-zinc-600">등록된 거래가 없습니다.</p> : null}
        {detail.transactions.length > 0 && <section>
          <h3 className="mb-2 font-semibold">수입·지출</h3>
          <ul className="space-y-2">
            {detail.transactions.map((transaction) => <li key={transaction.transactionUid} className="rounded-lg border border-zinc-200 p-3 text-sm">
              <div className="flex justify-between gap-2 font-medium">
                <span>{transaction.transactionType === "INCOME" ? "수입" : "지출"} · {transaction.categoryName}</span>
                <span>{transaction.transactionType === "INCOME" ? "+" : "−"}{formatMoney(transaction.amount)}</span>
              </div>
              <p className="mt-1 text-zinc-600">{transaction.accountName}{transaction.memo && ` · ${transaction.memo}`}</p>
            </li>)}
          </ul>
        </section>}
        {detail.transfers.length > 0 && <section>
          <h3 className="mb-2 font-semibold">이체</h3>
          <ul className="space-y-2">
            {detail.transfers.map((transfer) => <li key={transfer.transferUid} className="rounded-lg border border-zinc-200 p-3 text-sm">
              <div className="flex justify-between gap-2 font-medium"><span>이체 · {transfer.fromAccountName} → {transfer.toAccountName}</span><span>{formatMoney(transfer.amount)}</span></div>
              {transfer.memo && <p className="mt-1 text-zinc-600">{transfer.memo}</p>}
            </li>)}
          </ul>
        </section>}
      </div>}
  </DialogShell>;
}
