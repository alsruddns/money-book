"use client";

import { useState } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthNavigation } from "../hooks/useMonthNavigation";
import { useMonthlyTransactions } from "../hooks/useMonthlyTransactions";
import MonthSelector from "./MonthSelector";
import TransactionRow from "./TransactionRow";
import TransactionFormDialog from "./TransactionFormDialog";
import TransactionDetailDialog from "./TransactionDetailDialog";
import { useSearchParams } from "next/navigation";
import { useTransactionSearch } from "../hooks/useTransactionSearch";
import { useTransactionSearchOptions } from "../hooks/useTransactionSearchOptions";
import { defaultTransactionFilters, isValidSearchAmount, isValidSearchDateRange, type TransactionFilters } from "../search";

function TransactionListContent({ moneyBookUid }: { moneyBookUid: number }) {
  const [isCreateOpen, setCreateOpen] = useState(false);
  const [selectedTransactionUid, setSelectedTransactionUid] = useState<number | null>(null);
  const { canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { year, month, moveMonth } = useMonthNavigation();
  const isSearch = useSearchParams().get("search") === "1";
  const { transactions, isLoading, isError, errorMessage } = useMonthlyTransactions(moneyBookUid, year, month, !isSearch);
  const search = useTransactionSearch(moneyBookUid, year, month, isSearch);
  const [draft, setDraft] = useState<TransactionFilters>(() => defaultTransactionFilters(year, month));
  const [isFilterOpen, setFilterOpen] = useState(isSearch);
  const options = useTransactionSearchOptions(moneyBookUid, draft.transactionType, isSearch && isFilterOpen);
  const shownTransactions = isSearch ? search.result?.content ?? [] : transactions;
  const shownLoading = isSearch ? search.isLoading : isLoading;
  const shownError = isSearch ? search.isError : isError;
  const shownErrorMessage = isSearch ? search.errorMessage : errorMessage;
  function change<K extends keyof TransactionFilters>(key: K, value: TransactionFilters[K]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }
  function applySearch() { search.update(draft, true); }
  function resetSearch() {
    const clean = defaultTransactionFilters(year, month);
    setDraft(clean);
    search.update(clean, true);
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div><h1 className="text-2xl font-semibold">거래 내역</h1><p className="mt-1 text-sm text-zinc-600">월별 수입과 지출을 확인합니다.</p></div>
        {canCreate && <button type="button" onClick={() => setCreateOpen(true)}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">거래 추가</button>}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <MonthSelector year={year} month={month} onPrevious={() => moveMonth(-1)} onNext={() => moveMonth(1)} />
        <div className="flex gap-2"><button type="button" onClick={() => isSearch ? setFilterOpen((open) => !open) : search.update(search.filters)} aria-pressed={isFilterOpen} aria-expanded={isFilterOpen}
          className={`min-h-11 rounded-lg border px-4 text-sm ${isSearch ? "border-blue-600 bg-blue-50 text-blue-800" : "border-zinc-300 bg-white"}`}>검색/필터</button>
          {isSearch && <button type="button" onClick={search.showMonthly} className="min-h-11 rounded-lg border px-4 text-sm">월별 보기</button>}</div>
      </div>
      {isSearch && isFilterOpen && <section aria-label="거래 검색 필터" className="rounded-xl border border-zinc-200 bg-white p-4">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm">시작일<input aria-label="시작일" type="date" value={draft.startDate} onChange={(e) => change("startDate", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">종료일<input aria-label="종료일" type="date" value={draft.endDate} onChange={(e) => change("endDate", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">유형<select aria-label="거래 유형" value={draft.transactionType} onChange={(e) => change("transactionType", e.target.value as TransactionFilters["transactionType"])} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">전체</option><option value="INCOME">수입</option><option value="EXPENSE">지출</option></select></label>
          <label className="text-sm">카테고리<select aria-label="카테고리" value={draft.categoryUid} onChange={(e) => change("categoryUid", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">전체</option>{options.categories.map((item) => <option key={item.categoryUid} value={item.categoryUid}>{item.name}</option>)}</select></label>
          <label className="text-sm">계좌<select aria-label="계좌" value={draft.accountUid} onChange={(e) => change("accountUid", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">전체</option>{options.accounts.map((item) => <option key={item.accountUid} value={item.accountUid}>{item.name}</option>)}</select></label>
          <label className="text-sm">검색어<input aria-label="검색어" maxLength={200} value={draft.keyword} onChange={(e) => change("keyword", e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") applySearch(); }} className="mt-1 min-h-11 w-full rounded-lg border px-3" placeholder="메모, 카테고리, 계좌" /></label>
          <label className="text-sm">최소 금액<input aria-label="최소 금액" type="number" min="0" step="0.01" value={draft.minAmount} onChange={(e) => change("minAmount", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">최대 금액<input aria-label="최대 금액" type="number" min="0" step="0.01" value={draft.maxAmount} onChange={(e) => change("maxAmount", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">정렬<select aria-label="정렬" value={draft.sort} onChange={(e) => change("sort", e.target.value as TransactionFilters["sort"])} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="DATE_DESC">최신순</option><option value="DATE_ASC">오래된순</option><option value="AMOUNT_DESC">금액 높은순</option><option value="AMOUNT_ASC">금액 낮은순</option></select></label>
          <label className="text-sm">페이지 크기<select aria-label="페이지 크기" value={draft.size} onChange={(e) => change("size", Number(e.target.value))} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value={20}>20개</option><option value={50}>50개</option><option value={100}>100개</option></select></label>
        </div>
        {!isValidSearchDateRange(draft.startDate, draft.endDate) && <p role="alert" className="mt-3 text-sm text-red-700">조회 기간은 필수이며 2년 범위 이내로 지정해주세요.</p>}
        {!isValidSearchAmount(draft.minAmount) || !isValidSearchAmount(draft.maxAmount) || draft.minAmount && draft.maxAmount && Number(draft.minAmount) > Number(draft.maxAmount)
          ? <p role="alert" className="mt-3 text-sm text-red-700">금액은 0 이상이며 최소 금액은 최대 금액보다 클 수 없습니다.</p> : null}
        <div className="mt-4 flex flex-wrap gap-2"><button type="button" onClick={applySearch} disabled={!isValidSearchDateRange(draft.startDate, draft.endDate) || !isValidSearchAmount(draft.minAmount) || !isValidSearchAmount(draft.maxAmount) || Boolean(draft.minAmount && draft.maxAmount && Number(draft.minAmount) > Number(draft.maxAmount))} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:opacity-50">검색</button><button type="button" onClick={resetSearch} className="min-h-11 rounded-lg border px-4 text-sm">초기화</button></div>
      </section>}
      {shownLoading ? <p role="status">거래를 불러오는 중...</p> :
        shownError ? <p role="alert" className="text-red-600">{shownErrorMessage}</p> :
        shownTransactions.length === 0 ? <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-center">
          <p>{isSearch ? "조건에 맞는 거래가 없습니다." : "이 달에 등록된 거래가 없습니다."}</p>
          {!isSearch && canCreate && <button type="button" onClick={() => setCreateOpen(true)}
            className="mt-4 min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">거래 추가</button>}
        </div> : <ul className="space-y-3">
          {shownTransactions.map((transaction) => <TransactionRow key={transaction.transactionUid} transaction={transaction}
            onSelect={() => setSelectedTransactionUid(transaction.transactionUid)} />)}
        </ul>}
      {isSearch && search.result && search.result.totalPages > 1 && <nav aria-label="거래 검색 페이지" className="flex items-center justify-center gap-3">
        <button type="button" aria-label="이전 페이지" disabled={search.result.first} onClick={() => search.update({ ...search.filters, page: Math.max(0, search.filters.page - 1) })} className="min-h-11 rounded-lg border px-4 disabled:opacity-50">이전</button>
        <span aria-live="polite">{search.result.totalPages === 0 ? 0 : search.result.page + 1} / {search.result.totalPages}</span>
        <button type="button" aria-label="다음 페이지" disabled={search.result.last} onClick={() => search.update({ ...search.filters, page: search.filters.page + 1 })} className="min-h-11 rounded-lg border px-4 disabled:opacity-50">다음</button>
      </nav>}
      {isCreateOpen && <TransactionFormDialog moneyBookUid={moneyBookUid}
        onClose={() => setCreateOpen(false)} onSaved={() => setCreateOpen(false)} />}
      {selectedTransactionUid !== null && <TransactionDetailDialog moneyBookUid={moneyBookUid}
        transactionUid={selectedTransactionUid} canUpdate={canUpdate} canDelete={canDelete}
        onClose={() => setSelectedTransactionUid(null)} />}
    </div>
  );
}

export default function TransactionList({ moneyBookUid }: { moneyBookUid: number }) {
  const queryKey = useSearchParams().toString();
  return <TransactionListContent key={queryKey} moneyBookUid={moneyBookUid} />;
}
