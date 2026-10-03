"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { formatMoney } from "@/common/format/money";
import { useExpenseRanking } from "../hooks/useExpenseRanking";

function selectedPeriod(search: URLSearchParams) {
  const now = new Date();
  const parsedYear = Number(search.get("year"));
  const parsedMonth = Number(search.get("month"));
  return {
    periodType: search.get("periodType") === "YEAR" ? "YEAR" as const : "MONTH" as const,
    year: Number.isInteger(parsedYear) && parsedYear >= 1 && parsedYear <= 9998 ? parsedYear : now.getFullYear(),
    month: Number.isInteger(parsedMonth) && parsedMonth >= 1 && parsedMonth <= 12 ? parsedMonth : now.getMonth() + 1,
  };
}

export default function ExpenseRankingView({ moneyBookUid }: { moneyBookUid: number }) {
  const router = useRouter();
  const search = useSearchParams();
  const { periodType, year, month } = selectedPeriod(new URLSearchParams(search.toString()));
  const data = useExpenseRanking(moneyBookUid, periodType, year, month);
  const root = `/books/${moneyBookUid}/reports/expense-ranking`;
  function navigate(nextType: "MONTH" | "YEAR", nextYear = year, nextMonth = month) {
    router.push(`${root}?periodType=${nextType}&year=${nextYear}${nextType === "MONTH" ? `&month=${nextMonth}` : ""}`);
  }
  function moveMonth(offset: -1 | 1) {
    const shifted = new Date(year, month - 1 + offset, 1);
    navigate("MONTH", shifted.getFullYear(), shifted.getMonth() + 1);
  }
  const yearOptions = Array.from({ length: 9 }, (_unused, index) => year - 4 + index).filter((item) => item >= 1 && item <= 9998);

  if (data.permission.isLoading) return <p role="status">가계부 권한을 확인하는 중...</p>;
  if (!data.permission.canRead) return <p role="alert" className="rounded-xl border bg-white p-5">지출 순위를 조회할 권한이 없습니다.</p>;
  return <main className="space-y-5">
    <header><h1 className="text-2xl font-semibold">지출 순위</h1><p className="mt-1 text-sm text-zinc-600">금액이 큰 지출 거래를 최대 20건까지 보여줍니다.</p></header>
    <div className="flex flex-wrap items-center gap-2" aria-label="순위 기간 선택">
      <button type="button" aria-pressed={periodType === "MONTH"} onClick={() => navigate("MONTH")} className={`min-h-11 rounded-lg border px-4 ${periodType === "MONTH" ? "border-blue-600 bg-blue-50 text-blue-800" : "bg-white"}`}>월간</button>
      <button type="button" aria-pressed={periodType === "YEAR"} onClick={() => navigate("YEAR")} className={`min-h-11 rounded-lg border px-4 ${periodType === "YEAR" ? "border-blue-600 bg-blue-50 text-blue-800" : "bg-white"}`}>연간</button>
      {periodType === "MONTH" ? <div className="ml-0 flex items-center gap-2 sm:ml-3"><button type="button" aria-label="이전 달" onClick={() => moveMonth(-1)} className="min-h-11 rounded-lg border bg-white px-3">〈 이전</button><strong>{year}년 {month}월</strong><button type="button" aria-label="다음 달" onClick={() => moveMonth(1)} className="min-h-11 rounded-lg border bg-white px-3">다음 〉</button></div>
        : <label className="ml-0 text-sm sm:ml-3">조회 연도<select aria-label="조회 연도" value={year} onChange={(event) => navigate("YEAR", Number(event.target.value))} className="ml-2 min-h-11 rounded-lg border bg-white px-3">{yearOptions.map((option) => <option key={option} value={option}>{option}년</option>)}</select></label>}
    </div>
    {data.isLoading && !data.ranking ? <div role="status" className="space-y-2" aria-label="지출 순위 불러오는 중">{Array.from({ length: 5 }, (_unused, index) => <div key={index} className="h-16 animate-pulse rounded-lg bg-zinc-100" />)}</div>
      : data.isError && !data.ranking ? <div role="alert" className="rounded-xl border bg-white p-5 text-red-700"><p>{data.errorMessage}</p><button type="button" onClick={() => void data.refetch()} className="mt-3 min-h-10 rounded-lg border px-3">다시 시도</button></div>
      : data.ranking && <section aria-label="지출 순위 목록" className="overflow-hidden rounded-xl border bg-white">
        {data.isFetching && <p role="status" className="border-b px-4 py-2 text-xs text-zinc-600">최신 순위를 확인하고 있습니다.</p>}
        {data.ranking.length === 0 ? <p className="p-5 text-sm text-zinc-600">선택한 기간에 지출 내역이 없습니다.</p> : <>
          <div className="hidden overflow-x-auto md:block"><table className="w-full min-w-[42rem] text-left text-sm"><thead className="bg-zinc-50 text-zinc-600"><tr>{["순위", "날짜", "카테고리", "내용", "계좌/결제수단", "금액"].map((label) => <th key={label} scope="col" className="px-4 py-3 font-medium">{label}</th>)}</tr></thead>
            <tbody className="divide-y">{data.ranking.map((item) => <tr key={item.transactionUid}><td className="px-4 py-3 font-semibold">{item.rank}위</td><td className="px-4 py-3">{item.transactionDate}</td><td className="px-4 py-3">{item.categoryName}</td><td className="max-w-48 truncate px-4 py-3">{item.memo || "-"}</td><td className="px-4 py-3">{item.accountName}</td><td className="whitespace-nowrap px-4 py-3 text-right font-semibold">{formatMoney(item.amount)}</td></tr>)}</tbody>
          </table></div>
          <ol className="divide-y md:hidden">{data.ranking.map((item) => <li key={item.transactionUid} className="flex items-start justify-between gap-3 p-4"><div className="min-w-0"><p className="font-semibold">{item.rank}위 · {item.memo || item.categoryName}</p><p className="mt-1 truncate text-sm text-zinc-600">{item.categoryName} · {item.transactionDate}</p><p className="truncate text-xs text-zinc-500">{item.accountName}</p></div><strong className="shrink-0">{formatMoney(item.amount)}</strong></li>)}</ol>
        </>}
      </section>}
    <Link href={`/books/${moneyBookUid}/reports/monthly?year=${year}&month=${month}`} className="inline-flex min-h-11 items-center rounded-lg border bg-white px-4 text-sm">월간 분석으로 돌아가기</Link>
  </main>;
}
