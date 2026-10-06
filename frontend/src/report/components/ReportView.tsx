"use client";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { formatCount, formatMoney } from "@/common/format/money";
import { formatPercent, formatFractionPercent } from "@/common/format/percent";
import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import { useReportData } from "../hooks/useReportData";

function percent(value: number | null): string {
  return value === null || !Number.isFinite(value) ? "비교 불가" : `${value > 0 ? "+" : ""}${formatPercent(value)}`;
}
function Change({ amount, rate }: { amount: number; rate: number | null }) {
  const direction = amount > 0 ? "증가" : amount < 0 ? "감소" : "변동 없음";
  return <p className="mt-1 text-sm text-zinc-600">전월 대비 {direction} · {amount > 0 ? "+" : ""}{formatMoney(amount)} ({percent(rate)})</p>;
}

export default function ReportView({ moneyBookUid }: { moneyBookUid: number }) {
  const pathname = usePathname();
  const yearly = pathname.endsWith("/yearly");
  const monthNav = useMonthNavigation();
  const router = useRouter();
  const query = useSearchParams();
  const parsedYear = Number(query.get("year"));
  const year = yearly && Number.isInteger(parsedYear) && parsedYear >= 1 && parsedYear <= 9998 ? parsedYear : monthNav.year;
  const [yearDraft, setYearDraft] = useState(String(year));
  const data = useReportData(moneyBookUid, year, monthNav.month, yearly);
  const monthly = data.monthly;
  const categoryGroups: [string, typeof data.expenseCategories][] = [["지출 카테고리", data.expenseCategories], ["수입 카테고리", data.incomeCategories]];
  const root = `/books/${moneyBookUid}/reports`;
  if (data.permission.isLoading) return <p role="status">권한을 확인하는 중...</p>;
  if (!data.permission.canRead) return <p role="alert" className="rounded-xl border p-5">리포트를 조회할 권한이 없습니다.</p>;
  return <main className="min-w-0 space-y-6">
    <header><h1 className="text-2xl font-semibold">리포트</h1><p className="mt-1 text-sm text-zinc-600">가계부의 수입과 지출 흐름을 살펴봅니다.</p></header>
    {yearly ? <div className="flex flex-wrap items-end gap-3"><label className="text-sm">조회 연도<input aria-label="조회 연도" type="number" min="1" max="9998" value={yearDraft} onChange={(event) => setYearDraft(event.target.value)} className="mt-1 block min-h-11 rounded-lg border px-3" /></label><button type="button" onClick={() => { const next = Number(yearDraft); if (Number.isInteger(next) && next >= 1 && next <= 9998) router.push(`${root}/yearly?year=${next}`); }} className="min-h-11 rounded-lg bg-blue-600 px-4 text-white">연도 조회</button><span className="font-semibold">{year}년</span></div>
      : <div className="flex flex-wrap items-center gap-3"><button type="button" aria-label="이전 달" onClick={() => monthNav.moveMonth(-1)} className="min-h-11 rounded-lg border px-4">〈 이전</button><span className="font-semibold">{monthNav.year}년 {monthNav.month}월</span><button type="button" aria-label="다음 달" onClick={() => monthNav.moveMonth(1)} className="min-h-11 rounded-lg border px-4">다음 〉</button></div>}
    {data.isLoading ? <p role="status">리포트를 불러오는 중...</p> : data.isError ? <p role="alert" className="text-red-700">{data.errorMessage}</p> : yearly ? data.annual && <section className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-3">{[["연간 수입", data.annual.totalIncome], ["연간 지출", data.annual.totalExpense], ["연간 차액", data.annual.balance]].map(([label, amount]) => <article key={String(label)} className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">{label}</h2><p className="mt-2 text-xl font-semibold">{formatMoney(Number(amount))}</p></article>)}</div>
      <div className="space-y-3">{data.annual.months.map((item) => { const max = Math.max(item.income, item.expense, 1); return <article key={item.month} className="rounded-xl border bg-white p-4"><div className="flex justify-between font-medium"><span>{item.month}월</span><span>{formatCount(item.transactionCount)}</span></div><p className="mt-2 text-sm">수입 {formatMoney(item.income)}</p><div role="img" aria-label={`${item.month}월 수입 ${formatMoney(item.income)}`} className="mt-1 h-2 rounded bg-blue-600" style={{ width: `${Math.max(2, item.income / max * 100)}%` }} /><p className="mt-2 text-sm">지출 {formatMoney(item.expense)}</p><div role="img" aria-label={`${item.month}월 지출 ${formatMoney(item.expense)}`} className="mt-1 h-2 rounded bg-rose-500" style={{ width: `${Math.max(2, item.expense / max * 100)}%` }} /><p className="mt-2 text-sm text-zinc-600">차액 {formatMoney(item.balance)}</p></article>; })}</div>
    </section> : monthly && <section className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{([["수입", monthly.income], ["지출", monthly.expense], ["차액", monthly.balance], ["거래 건수", `${formatCount(monthly.transactionCount)}`]] as [string, number | string][]).map(([label, value]) => <article key={label} className="rounded-xl border bg-white p-4"><h2 className="text-sm text-zinc-600">{label}</h2><p className="mt-2 text-xl font-semibold">{typeof value === "number" ? formatMoney(value) : value}</p>{label === "수입" && <Change amount={monthly.incomeChange} rate={monthly.incomeChangeRate} />}{label === "지출" && <Change amount={monthly.expenseChange} rate={monthly.expenseChangeRate} />}</article>)}</div>
      <article className="rounded-xl border bg-white p-4"><h2 className="font-semibold">예산 대비 실적</h2>{!monthly.budgetConfigured ? <p className="mt-2 text-sm text-zinc-600">이 달에는 총 예산이 설정되지 않았습니다.</p> : <><div className="mt-3 flex flex-wrap justify-between gap-2 text-sm"><span>예산 {formatMoney(monthly.totalBudget)}</span><span>지출 {formatMoney(monthly.expense)}</span><span>남은 예산 {formatMoney(monthly.remainingBudget)}</span></div><p className="mt-2">{monthly.overBudget ? "예산 초과" : "사용률"} · {formatPercent(monthly.budgetUsageRate)}</p><div className="mt-2 h-3 overflow-hidden rounded-full bg-zinc-100"><div className={`h-full ${monthly.overBudget ? "bg-red-600" : "bg-blue-600"}`} style={{ width: `${Math.min(100, Math.max(0, Number.isFinite(monthly.budgetUsageRate) ? monthly.budgetUsageRate ?? 0 : 0))}%` }} /></div></>}</article>
      <div className="grid gap-5 xl:grid-cols-2">{categoryGroups.map(([title, rows]) => <section key={title} className="space-y-2"><h2 className="font-semibold">{title}</h2>{rows.length === 0 ? <p className="rounded-xl border bg-white p-4 text-sm text-zinc-600">표시할 내역이 없습니다.</p> : rows.map((item) => <article key={item.categoryUid} className="rounded-xl border bg-white p-4"><div className="flex flex-wrap justify-between gap-2"><span className="min-w-0 break-words">{item.categoryName}</span><span className="shrink-0">{formatMoney(item.totalAmount)}</span></div><p className="mt-1 text-sm text-zinc-600">{formatFractionPercent(item.ratio, 1)} · {formatCount(item.transactionCount)}</p><div className="mt-2 h-2 rounded-full bg-zinc-100"><div className="h-full rounded-full bg-blue-600" style={{ width: `${Math.min(100, Math.max(0, item.ratio * 100))}%` }} /></div></article>)}</section>)}</div>
      <section className="space-y-2"><h2 className="font-semibold">계좌별 요약</h2>{data.accounts.map((account) => <article key={account.accountUid} className="grid gap-2 rounded-xl border bg-white p-4 sm:grid-cols-2"><strong>{account.accountName}</strong><span>수입 {formatMoney(account.incomeAmount)}</span><span>지출 {formatMoney(account.expenseAmount)}</span><span>이체 들어옴 {formatMoney(account.transferInAmount)}</span><span>이체 나감 {formatMoney(account.transferOutAmount)}</span><span>순변동 {formatMoney(account.netChange)}</span></article>)}</section>
    </section>}
    <Link href={`/books/${moneyBookUid}/closings?year=${monthNav.year}&month=${monthNav.month}`} className="inline-flex min-h-11 items-center rounded-lg border px-4">월 결산 보기</Link>
  </main>;
}
