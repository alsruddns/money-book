"use client";

import Link from "next/link";
import MonthSelector from "@/transaction/components/MonthSelector";
import { formatCount, formatMoney } from "@/common/format/money";
import { formatPercentPoints, formatFractionPercent } from "@/common/format/percent";
import { useMonthlyDashboard } from "@/dashboard/hooks/useMonthlyDashboard";

function Comparison({ value }: { value: number | null }) {
  return <p className="mt-1 text-xs text-zinc-600">전월 대비 {value === null ? "비교 불가" : `${value > 0 ? "+" : ""}${formatPercentPoints(value)}`}</p>;
}

export default function MoneyBookDetail({ moneyBookUid }: { moneyBookUid: number }) {
  const data = useMonthlyDashboard(moneyBookUid);
  const { year, month, dashboard, permission } = data;
  const root = `/books/${moneyBookUid}`;
  if (permission.isLoading) return <p role="status" className="rounded-xl border bg-white p-5">가계부를 불러오는 중...</p>;
  if (permission.isError) return <p role="alert" className="rounded-xl border bg-white p-5 text-red-700">{permission.errorMessage}</p>;
  if (!permission.moneyBook) return <p role="alert" className="rounded-xl border bg-white p-5">접근 가능한 가계부를 찾을 수 없습니다.</p>;
  if (!permission.canRead) return <p role="alert" className="rounded-xl border bg-white p-5">가계부 조회 권한이 없습니다.</p>;
  const trendMax = Math.max(1, ...(dashboard?.monthlyTrend.flatMap((item) => [item.income, item.expense]) ?? []));

  return <main className="min-w-0 space-y-6">
    <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="text-sm text-zinc-600">{permission.moneyBook.name}</p><h1 className="mt-1 text-2xl font-semibold">대시보드</h1>
        <p className="mt-1 text-sm text-zinc-600">선택한 달의 수입과 지출 현황입니다.</p></div>
      <div className="flex flex-wrap items-center gap-2"><MonthSelector year={year} month={month} allowPrevious={year > 2} allowNext={year < 9998} onPrevious={() => data.moveMonth(-1)} onNext={() => data.moveMonth(1)} />
        <button type="button" onClick={data.goToToday} className="min-h-11 rounded-lg border px-3 text-sm">이번 달</button></div>
    </header>
    {data.isLoading && !dashboard ? <div role="status" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="대시보드 불러오는 중">{[1, 2, 3, 4].map((item) => <div key={item} className="h-24 animate-pulse rounded-xl bg-zinc-100" />)}</div>
      : data.isError && !dashboard ? <div role="alert" className="rounded-xl border bg-white p-5 text-red-700"><p>{data.errorMessage}</p><button type="button" onClick={() => void data.refetch()} className="mt-3 min-h-10 rounded-lg border px-3">다시 시도</button></div>
      : dashboard && <>
        {data.isFetching && <p role="status" className="text-xs text-zinc-500">최신 자료를 확인하고 있습니다.</p>}
        <section aria-label="월별 요약" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <article className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">수입</h2><p className="mt-2 text-xl font-semibold text-blue-700">{formatMoney(dashboard.summary.totalIncome)}</p><Comparison value={dashboard.comparison.incomeChangeRate} /></article>
          <article className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">지출</h2><p className="mt-2 text-xl font-semibold text-rose-700">{formatMoney(dashboard.summary.totalExpense)}</p><Comparison value={dashboard.comparison.expenseChangeRate} /></article>
          <article className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">잔액</h2><p className="mt-2 text-xl font-semibold">{formatMoney(dashboard.summary.balance)}</p><p className="mt-1 text-xs text-zinc-600">수입에서 지출을 뺀 금액</p></article>
          <article className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">거래 건수</h2><p className="mt-2 text-xl font-semibold">{formatCount(dashboard.summary.transactionCount)}</p><p className="mt-1 text-xs text-zinc-600">수입 {formatCount(dashboard.summary.incomeCount)} · 지출 {formatCount(dashboard.summary.expenseCount)}</p></article>
        </section>
        {dashboard.summary.transactionCount === 0 && <p className="rounded-lg bg-blue-50 px-4 py-3 text-sm text-blue-900">선택한 달에 등록된 거래가 없습니다. 요약과 예산 현황은 계속 확인할 수 있습니다.</p>}

        <section className="grid gap-5 xl:grid-cols-2">
          <article className="min-w-0 rounded-xl border bg-white p-4 sm:p-5"><h2 className="font-semibold">최근 6개월 수입·지출</h2>
            <p className="mt-1 text-xs text-zinc-600">막대는 월별 합계이며 금액은 원화 기준입니다.</p>
            <div role="img" aria-label="최근 6개월 수입 및 지출 비교" className="mt-5 grid grid-cols-6 gap-2">
              {dashboard.monthlyTrend.map((item) => <div key={`${item.year}-${item.month}`} className="flex min-w-0 flex-col items-center gap-2">
                <div className="flex h-36 w-full items-end justify-center gap-1" title={`${item.year}년 ${item.month}월: 수입 ${formatMoney(item.income)}, 지출 ${formatMoney(item.expense)}`}>
                  <span className="w-3 rounded-t bg-blue-600 sm:w-5" style={{ height: `${item.income > 0 ? Math.max(3, item.income / trendMax * 100) : 0}%` }} />
                  <span className="w-3 rounded-t bg-rose-500 sm:w-5" style={{ height: `${item.expense > 0 ? Math.max(3, item.expense / trendMax * 100) : 0}%` }} />
                </div><span className="text-xs">{item.month}월</span>
              </div>)}
            </div><div className="mt-3 flex gap-4 text-xs"><span><i className="mr-1 inline-block size-2 rounded-full bg-blue-600" />수입</span><span><i className="mr-1 inline-block size-2 rounded-full bg-rose-500" />지출</span></div>
            <ul className="mt-4 space-y-1 text-sm">{dashboard.monthlyTrend.map((item) => <li key={`${item.year}-${item.month}`} className="flex justify-between gap-3"><span>{item.year}년 {item.month}월</span><span>수입 {formatMoney(item.income)} · 지출 {formatMoney(item.expense)}</span></li>)}</ul>
          </article>

          <article className="rounded-xl border bg-white p-4 sm:p-5"><h2 className="font-semibold">카테고리별 지출</h2>
            {dashboard.categoryExpenses.length === 0 ? <p className="mt-3 text-sm text-zinc-600">선택한 달에 등록된 지출이 없습니다.</p> : <ul className="mt-3 space-y-3">{dashboard.categoryExpenses.map((item) => <li key={item.categoryUid}>
              <div className="flex justify-between gap-3 text-sm"><span className="font-medium">{item.categoryName}</span><span>{formatMoney(item.amount)}</span></div>
              <div className="mt-1 flex items-center gap-2"><div className="h-2 flex-1 overflow-hidden rounded-full bg-zinc-100"><div className="h-full rounded-full bg-indigo-500" style={{ width: `${Math.min(100, Math.max(0, item.ratio * 100))}%` }} /></div><span className="w-14 text-right text-xs text-zinc-600">{formatFractionPercent(item.ratio)}</span></div>
              <p className="mt-1 text-xs text-zinc-500">{formatCount(item.transactionCount)}</p>
            </li>)}</ul>}
          </article>
        </section>

        <section className="rounded-xl border bg-white p-4 sm:p-5"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="font-semibold">예산 현황</h2><Link href={`${root}/budgets?year=${year}&month=${month}`} className="text-sm text-blue-700 hover:underline">예산 관리</Link></div>
          {!dashboard.budget ? <p className="mt-3 text-sm text-zinc-600">이번 달 예산이 설정되지 않았습니다.</p> : <>
            <div className="mt-3 grid gap-3 text-sm sm:grid-cols-3"><p>예산<strong className="mt-1 block">{dashboard.budget.totalBudget === null ? "총액 미설정" : formatMoney(dashboard.budget.totalBudget)}</strong></p><p>사용<strong className="mt-1 block">{formatMoney(dashboard.budget.actualExpense)}</strong></p><p>남음<strong className="mt-1 block">{dashboard.budget.remaining === null ? "-" : dashboard.budget.overBudget ? `${formatMoney(Math.abs(dashboard.budget.remaining))} 초과` : formatMoney(dashboard.budget.remaining)}</strong></p></div>
            <p className="mt-3 text-sm">{dashboard.budget.overBudget ? "예산 초과 · " : "사용률 · "}{dashboard.budget.usageRate === null ? "-" : formatPercentPoints(dashboard.budget.usageRate)}</p>
            {dashboard.budget.usageRate !== null && <div className="mt-2 h-2 overflow-hidden rounded-full bg-zinc-100"><div className={`h-full ${dashboard.budget.overBudget ? "bg-rose-600" : "bg-blue-600"}`} style={{ width: `${Math.min(100, Math.max(0, dashboard.budget.usageRate))}%` }} /></div>}
          </>}
        </section>

        <section className="rounded-xl border bg-white p-4 sm:p-5"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="font-semibold">이번 달 지출 TOP 5</h2><Link href={`${root}/reports/expense-ranking?periodType=MONTH&year=${year}&month=${month}`} className="text-sm font-medium text-blue-700 hover:underline">전체 지출 순위 보기</Link></div>
          {dashboard.topExpenses.length === 0 ? <p className="mt-3 text-sm text-zinc-600">선택한 달에 등록된 지출이 없습니다.</p> : <ol className="mt-3 divide-y">{dashboard.topExpenses.map((item) => <li key={item.transactionUid} className="flex items-center justify-between gap-3 py-3 text-sm"><div className="min-w-0"><p className="truncate font-medium">{item.rank}. {item.memo || item.categoryName}</p><p className="truncate text-xs text-zinc-600">{item.categoryName} · {item.transactionDate}</p></div><strong className="shrink-0">{formatMoney(item.amount)}</strong></li>)}</ol>}
        </section>
        <nav aria-label="가계부 바로가기" className="flex flex-wrap gap-2">{[["캘린더", `${root}/calendar?year=${year}&month=${month}`], ["거래내역", `${root}/transactions?year=${year}&month=${month}`], ["예산", `${root}/budgets?year=${year}&month=${month}`]].map(([label, href]) => <Link key={href} href={href} className="inline-flex min-h-11 items-center rounded-lg border bg-white px-4 text-sm">{label}</Link>)}</nav>
      </>}
  </main>;
}
