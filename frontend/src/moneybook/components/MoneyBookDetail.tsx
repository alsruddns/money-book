"use client";

import Link from "next/link";
import { formatCount, formatMoney } from "@/common/format/money";
import BudgetProgress from "@/budget/components/BudgetProgress";
import { useMonthlyDashboard } from "@/dashboard/hooks/useMonthlyDashboard";

export default function MoneyBookDetail({ moneyBookUid }: { moneyBookUid: number }) {
  const { year, month, isMonthReady, permission, calendar, budget, activeDays } = useMonthlyDashboard(moneyBookUid);
  const root = `/books/${moneyBookUid}`;
  if (permission.isLoading) return <p role="status">가계부를 불러오는 중...</p>;
  if (permission.isError) return <p role="alert" className="text-red-600">{permission.errorMessage}</p>;
  if (!permission.moneyBook) return <p role="alert">접근 가능한 가계부를 찾을 수 없습니다.</p>;
  if (!isMonthReady) return <p role="status">대시보드를 불러오는 중...</p>;

  return <div className="space-y-6">
    <header>
      <p className="text-sm text-zinc-600">{permission.moneyBook.name}</p>
      <h1 className="mt-1 text-2xl font-semibold">{year}년 {month}월 대시보드</h1>
      <p className="mt-1 text-sm text-zinc-600">이번 달 수입, 지출과 예산을 한눈에 확인합니다.</p>
    </header>
    {!permission.canRead ? <p role="alert">가계부 조회 권한이 없습니다.</p> : <>
      {calendar.isLoading ? <p role="status">월별 현황을 불러오는 중...</p> : calendar.isError ?
        <p role="alert" className="text-red-600">{calendar.errorMessage}</p> : calendar.calendar && <>
          <section aria-label="월별 요약" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">이번 달 수입</p><p className="mt-2 text-xl font-semibold text-blue-700">{formatMoney(calendar.totals.income)}</p></div>
            <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">이번 달 지출</p><p className="mt-2 text-xl font-semibold text-red-700">{formatMoney(calendar.totals.expense)}</p></div>
            <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">수입 − 지출</p><p className="mt-2 text-xl font-semibold">{formatMoney(calendar.totals.income - calendar.totals.expense)}</p></div>
            <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">이체</p><p className="mt-2 text-xl font-semibold">{formatMoney(calendar.totals.transferOut)}</p></div>
          </section>
          <section className="rounded-xl border border-zinc-200 bg-white p-5">
            <div className="mb-3 flex items-center justify-between gap-3"><h2 className="text-lg font-semibold">최근 활동 날짜</h2><Link href={`${root}/calendar?year=${year}&month=${month}`} className="text-sm text-blue-700 hover:underline">캘린더 보기</Link></div>
            {activeDays.length === 0 ? <p className="text-sm text-zinc-600">이번 달 등록된 거래나 이체가 없습니다.</p> :
              <ul className="space-y-2 text-sm">{activeDays.map((day) => <li key={day.date} className="flex flex-wrap justify-between gap-2 border-t border-zinc-100 pt-2">
                <span>{day.date}</span><span>수입 {formatMoney(day.incomeAmount)} · 지출 {formatMoney(day.expenseAmount)}{day.transferCount > 0 && ` · 이체 ${formatCount(day.transferCount)}`}</span>
              </li>)}</ul>}
          </section>
        </>}
      {budget.isLoading ? <p role="status">예산 현황을 불러오는 중...</p> : budget.isError ?
        <p role="alert" className="text-red-600">{budget.errorMessage}</p> : budget.budget && <section className="rounded-xl border border-zinc-200 bg-white p-5">
          <div className="mb-3 flex items-center justify-between gap-3"><h2 className="text-lg font-semibold">예산 현황</h2><Link href={`${root}/budgets?year=${year}&month=${month}`} className="text-sm text-blue-700 hover:underline">예산 보기</Link></div>
          {!budget.budget.configured ? <p className="text-sm text-zinc-600">이번 달 예산이 아직 설정되지 않았습니다.</p> : <>
            <div className="mb-4 grid gap-3 text-sm sm:grid-cols-3">
              <p>총 예산 <strong className="block text-lg">{budget.budget.totalBudget === null ? "미설정" : formatMoney(budget.budget.totalBudget)}</strong></p>
              <p>실제 지출 <strong className="block text-lg">{formatMoney(budget.budget.totalExpense)}</strong></p>
              <p>남은 예산 <strong className="block text-lg">{budget.budget.remainingBudget === null ? "총 예산 미설정" : budget.budget.overBudget ? `${formatMoney(Math.abs(budget.budget.remainingBudget))} 초과` : formatMoney(budget.budget.remainingBudget)}</strong></p>
            </div>
            <BudgetProgress usageRate={budget.budget.usageRate} overBudget={budget.budget.overBudget} />
          </>}
        </section>}
      <nav aria-label="빠른 이동" className="flex flex-wrap gap-2">
        <Link href={`${root}/calendar?year=${year}&month=${month}`} className="inline-flex min-h-11 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium hover:bg-zinc-50">캘린더</Link>
        <Link href={`${root}/transactions?year=${year}&month=${month}`} className="inline-flex min-h-11 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium hover:bg-zinc-50">거래내역</Link>
        <Link href={`${root}/budgets?year=${year}&month=${month}`} className="inline-flex min-h-11 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium hover:bg-zinc-50">예산</Link>
      </nav>
    </>}
  </div>;
}
