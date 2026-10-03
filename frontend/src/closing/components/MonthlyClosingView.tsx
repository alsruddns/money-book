"use client";
import { useEffect, useState } from "react";
import { formatCount, formatMoney } from "@/common/format/money";
import { formatPercent } from "@/common/format/percent";
import { formatKoreaDateTime } from "@/common/format/dateTime";
import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import { useMonthlyClosing } from "../hooks/useMonthlyClosing";

function ClosingValues({ value, label }: { value: { income: number; expense: number; balance: number; transactionCount: number; budgetConfigured: boolean; totalBudget: number | null; remainingBudget: number | null; budgetUsageRate: number | null; overBudget: boolean; closedAt?: string; closedByUserUid?: number }; label: string }) {
  return <section className="rounded-xl border bg-white p-5"><h2 className="font-semibold">{label}</h2>{value.closedAt && <p className="mt-2 text-sm text-zinc-600">마감일 {formatKoreaDateTime(value.closedAt)} · 마감 사용자 UID {value.closedByUserUid}</p>}<dl className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3"><div><dt className="text-sm text-zinc-500">총 수입</dt><dd className="mt-1 font-semibold">{formatMoney(value.income)}</dd></div><div><dt className="text-sm text-zinc-500">총 지출</dt><dd className="mt-1 font-semibold">{formatMoney(value.expense)}</dd></div><div><dt className="text-sm text-zinc-500">차액</dt><dd className="mt-1 font-semibold">{formatMoney(value.balance)}</dd></div><div><dt className="text-sm text-zinc-500">거래 건수</dt><dd className="mt-1 font-semibold">{formatCount(value.transactionCount)}</dd></div><div><dt className="text-sm text-zinc-500">예산</dt><dd className="mt-1 font-semibold">{value.budgetConfigured ? formatMoney(value.totalBudget) : "설정되지 않음"}</dd></div>{value.budgetConfigured && <div><dt className="text-sm text-zinc-500">예산 실적</dt><dd className="mt-1 font-semibold">{value.overBudget ? `예산 초과 · ${formatMoney(Math.abs(value.remainingBudget ?? 0))}` : `남은 예산 ${formatMoney(value.remainingBudget)}`} · 사용률 {formatPercent(value.budgetUsageRate)}</dd></div>}</dl></section>;
}

export default function MonthlyClosingView({ moneyBookUid }: { moneyBookUid: number }) {
  const { year, month, moveMonth } = useMonthNavigation();
  const view = useMonthlyClosing(moneyBookUid, year, month);
  const [dialog, setDialog] = useState<"close" | "cancel" | null>(null);
  useEffect(() => {
    if (!dialog) return;
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === "Escape") setDialog(null); };
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [dialog]);
  const today = new Date();
  const isFuture = year > today.getFullYear() || year === today.getFullYear() && month > today.getMonth() + 1;
  if (view.permission.isLoading) return <p role="status">권한을 확인하는 중...</p>;
  if (!view.permission.canRead) return <p role="alert" className="rounded-xl border p-5">결산 정보를 조회할 권한이 없습니다.</p>;
  return <main className="min-w-0 space-y-5"><header><h1 className="text-2xl font-semibold">월 결산</h1><p className="mt-1 text-sm text-zinc-600">월별 실적 snapshot을 확인하고 관리합니다.</p></header>
    <div className="flex flex-wrap items-center gap-3"><button type="button" aria-label="이전 달" onClick={() => moveMonth(-1)} className="min-h-11 rounded-lg border px-4">〈 이전</button><h2 className="font-semibold">{year}년 {month}월</h2><button type="button" aria-label="다음 달" onClick={() => moveMonth(1)} className="min-h-11 rounded-lg border px-4">다음 〉</button></div>
    {view.isLoading ? <p role="status">결산 정보를 불러오는 중...</p> : view.isError ? <p role="alert" className="text-red-700">{view.errorMessage}</p> : view.closing ? <><p className="inline-flex rounded-full bg-emerald-50 px-3 py-1 text-sm font-medium text-emerald-800">마감 완료</p><p className="text-sm text-zinc-600">아래 값은 결산 당시 기록이며, 현재 데이터와 다를 수 있습니다. 마감 중에는 해당 월 거래·이체·예산 변경이 제한됩니다.</p><ClosingValues value={view.closing} label="결산 당시 snapshot" />{view.permission.canUpdate && <button type="button" onClick={() => setDialog("cancel")} className="min-h-11 rounded-lg border border-red-300 px-4 text-red-700">결산 취소</button>}</> : view.notClosed ? <><p className="font-medium">아직 결산하지 않았습니다.</p>{view.monthly && <ClosingValues value={view.monthly} label="현재 월 실적" />}{view.permission.canUpdate && !isFuture && <button type="button" onClick={() => setDialog("close")} className="min-h-11 rounded-lg bg-blue-600 px-4 font-medium text-white">{month}월 결산하기</button>}{isFuture && <p className="text-sm text-zinc-600">미래 월은 결산할 수 없습니다.</p>}</> : null}
    {view.errorMessage && !view.isError && <p role="alert" className="text-red-700">{view.errorMessage}</p>}
    <aside className="rounded-lg bg-zinc-50 p-4 text-sm text-zinc-600">이 API는 선택 월의 snapshot을 조회합니다. Backend에 결산 목록 API가 없어 월별 목록은 표시하지 않습니다.</aside>
    {dialog && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDialog(null); }}><section role="dialog" aria-modal="true" aria-labelledby="closing-dialog-title" className="w-full max-w-md rounded-xl bg-white p-5 shadow-xl"><h2 id="closing-dialog-title" className="text-lg font-semibold">{dialog === "close" ? `${year}년 ${month}월을 결산하시겠습니까?` : "결산을 취소하시겠습니까?"}</h2><p className="mt-3 text-sm text-zinc-600">{dialog === "close" ? "현재 시점의 수입·지출과 예산 현황을 snapshot으로 기록합니다. 마감된 달은 거래, 이체, 예산 변경이 제한되며 결산 취소 후 다시 변경할 수 있습니다." : "결산 기록만 삭제됩니다. 실제 거래와 예산 데이터는 삭제되지 않으며, 해당 월 변경 제한이 해제됩니다."}</p><div className="mt-5 flex justify-end gap-2"><button type="button" onClick={() => setDialog(null)} className="min-h-11 rounded-lg border px-4">취소</button><button type="button" disabled={view.isSaving} onClick={async () => { const success = dialog === "close" ? await view.close() : await view.cancel(); if (success) setDialog(null); }} className="min-h-11 rounded-lg bg-blue-600 px-4 text-white disabled:opacity-50">{view.isSaving ? "처리 중..." : dialog === "close" ? "결산" : "결산 취소"}</button></div></section></div>}
  </main>;
}
