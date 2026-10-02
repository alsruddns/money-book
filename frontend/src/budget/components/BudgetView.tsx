"use client";

import { useState } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import MonthSelector from "@/transaction/components/MonthSelector";
import { formatMoney } from "@/common/format/money";
import { useMonthlyBudget } from "../hooks/useMonthlyBudget";
import BudgetOverview from "./BudgetOverview";
import BudgetForm from "./BudgetForm";

export default function BudgetView({ moneyBookUid }: { moneyBookUid: number }) {
  const [isEditing, setEditing] = useState(false);
  const { canRead, canUpdate } = useMoneyBookPermission(moneyBookUid);
  const { year, month, moveMonth, goToToday } = useMonthNavigation();
  const { budget, isLoading, isFetching, isError, errorMessage } = useMonthlyBudget(moneyBookUid, year, month, canRead);
  function changeMonth(offset: -1 | 1) { setEditing(false); moveMonth(offset); }

  return <div className="space-y-5">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><h1 className="text-2xl font-semibold">월별 예산</h1><p className="mt-1 text-sm text-zinc-600">지출과 예산 사용 현황을 확인합니다.</p></div>
      <div className="flex flex-wrap items-center gap-2">
        <MonthSelector year={year} month={month} onPrevious={() => changeMonth(-1)} onNext={() => changeMonth(1)} />
        <button type="button" onClick={() => { setEditing(false); goToToday(); }} className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm">오늘</button>
      </div>
    </div>
    {!canRead ? <p role="alert">예산 조회 권한이 없습니다.</p> : isLoading ? <p role="status">예산을 불러오는 중...</p> :
      isError ? <p role="alert" className="text-red-600">{errorMessage}</p> : budget && <>
        {isFetching && <p role="status" className="text-sm text-zinc-600">예산을 새로고침하는 중...</p>}
        {!budget.configured && <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6">
          <p>{year}년 {month}월 예산이 아직 설정되지 않았습니다.</p>
          <p className="mt-1 text-sm text-zinc-600">이번 달 지출: {formatMoney(budget.totalExpense)}</p>
        </div>}
        {budget.configured && <BudgetOverview budget={budget} />}
        {canUpdate && (isEditing ? <BudgetForm key={`${year}-${month}`} moneyBookUid={moneyBookUid} year={year} month={month} budget={budget}
          onSaved={() => setEditing(false)} onCancel={() => setEditing(false)} /> :
          <button type="button" onClick={() => setEditing(true)} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">
            {budget.configured ? "예산 수정" : "예산 설정"}
          </button>)}
      </>}
  </div>;
}
