"use client";

import { useEffect, useState } from "react";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import DialogShell from "@/common/components/DialogShell";
import { formatMoney } from "@/common/format/money";
import { useCalendarDayDetail } from "../hooks/useCalendarDayDetail";
import type { CalendarDayResponse } from "../dto/res/MonthlyCalendarResponse";
import TransactionFormDialog from "@/transaction/components/TransactionFormDialog";
import TransactionDetailDialog from "@/transaction/components/TransactionDetailDialog";

const weekdayName: Record<CalendarDayResponse["dayOfWeek"], string> = {
  SUNDAY: "일요일", MONDAY: "월요일", TUESDAY: "화요일", WEDNESDAY: "수요일",
  THURSDAY: "목요일", FRIDAY: "금요일", SATURDAY: "토요일",
};

function formatCalendarDate(date: string, dayOfWeek: CalendarDayResponse["dayOfWeek"]): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  return match ? `${Number(match[1])}년 ${Number(match[2])}월 ${Number(match[3])}일 ${weekdayName[dayOfWeek]}` : date;
}

export default function CalendarDayDetailDialog({ moneyBookUid, day, canCreate, canUpdate, canDelete, onClose }: {
  moneyBookUid: number; day: CalendarDayResponse; canCreate: boolean; canUpdate: boolean; canDelete: boolean; onClose: () => void;
}) {
  const [creatingType, setCreatingType] = useState<TransactionType | null>(null);
  const [selectedTransactionUid, setSelectedTransactionUid] = useState<number | null>(null);
  const { detail, isLoading, isError, errorMessage } = useCalendarDayDetail(moneyBookUid, day.date);
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  if (creatingType) return <TransactionFormDialog moneyBookUid={moneyBookUid} initialDate={day.date} initialType={creatingType} onClose={() => setCreatingType(null)} onSaved={() => setCreatingType(null)} />;
  if (selectedTransactionUid !== null) return <TransactionDetailDialog moneyBookUid={moneyBookUid} transactionUid={selectedTransactionUid}
    canUpdate={canUpdate} canDelete={canDelete} onClose={() => setSelectedTransactionUid(null)} />;

  const income = Number(day.incomeAmount) || 0;
  const expense = Number(day.expenseAmount) || 0;
  return <DialogShell title={formatCalendarDate(day.date, day.dayOfWeek)} onClose={onClose}>
    {(detail?.holiday || day.holiday) && <p className="mb-4 text-sm font-medium text-red-700">공휴일 · {detail?.holidayName ?? day.holidayName}</p>}
    <section aria-label="선택한 날짜 요약" className="mb-5 grid grid-cols-3 gap-2 rounded-lg bg-zinc-50 p-3 text-sm">
      <p>수입<strong className="mt-1 block text-blue-700">{formatMoney(income)}</strong></p>
      <p>지출<strong className="mt-1 block text-red-700">{formatMoney(expense)}</strong></p>
      <p>잔액<strong className="mt-1 block">{formatMoney(income - expense)}</strong></p>
    </section>
    {canCreate && <div className="mb-4 flex gap-2">
      <button type="button" onClick={() => setCreatingType("INCOME")} className="min-h-11 flex-1 rounded-lg bg-blue-600 px-3 text-sm font-medium text-white">+ 수입 추가</button>
      <button type="button" onClick={() => setCreatingType("EXPENSE")} className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-3 text-sm font-medium">+ 지출 추가</button>
    </div>}
    {isLoading ? <p role="status">날짜별 내역을 불러오는 중...</p> :
      isError ? <p role="alert" className="text-red-600">{errorMessage}</p> : detail && <div className="space-y-5">
        {detail.transactions.length === 0 && detail.transfers.length === 0 ?
          <p className="text-sm text-zinc-600">이 날짜에는 거래가 없습니다.</p> : null}
        {detail.transactions.length > 0 && <section>
          <h3 className="mb-2 font-semibold">수입·지출</h3>
          <ul className="space-y-2">
            {detail.transactions.map((transaction) => <li key={transaction.transactionUid} className="rounded-lg border border-zinc-200 p-3 text-sm">
              <button type="button" onClick={() => setSelectedTransactionUid(transaction.transactionUid)} className="w-full text-left">
              <div className="flex justify-between gap-2 font-medium">
                <span>{transaction.transactionType === "INCOME" ? "수입" : "지출"} · {transaction.categoryName}</span>
                <span>{transaction.transactionType === "INCOME" ? "+" : "−"}{formatMoney(transaction.amount)}</span>
              </div>
              <p className="mt-1 text-zinc-600">{transaction.accountName}{transaction.memo && ` · ${transaction.memo}`}</p>
              {(canUpdate || canDelete) && <span className="mt-2 inline-block text-xs text-blue-700">거래 상세 및 관리</span>}
              </button>
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
