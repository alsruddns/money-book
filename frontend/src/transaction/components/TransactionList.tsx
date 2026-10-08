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
import { useTranslation } from "@/i18n/useTranslation";

function TransactionListContent({ moneyBookUid }: { moneyBookUid: number }) {
  const { t } = useTranslation();
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
        <div><h1 className="text-2xl font-semibold">{t("transactions.title")}</h1><p className="mt-1 text-sm text-zinc-600">{t("transactions.description")}</p></div>
        {canCreate && <button type="button" onClick={() => setCreateOpen(true)}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">{t("transactions.add")}</button>}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <MonthSelector year={year} month={month} onPrevious={() => moveMonth(-1)} onNext={() => moveMonth(1)} />
        <div className="flex gap-2"><button type="button" onClick={() => isSearch ? setFilterOpen((open) => !open) : search.update(search.filters)} aria-pressed={isFilterOpen} aria-expanded={isFilterOpen}
          className={`min-h-11 rounded-lg border px-4 text-sm ${isSearch ? "border-blue-600 bg-blue-50 text-blue-800" : "border-zinc-300 bg-white"}`}>{t("transactions.searchFilter")}</button>
          {isSearch && <button type="button" onClick={search.showMonthly} className="min-h-11 rounded-lg border px-4 text-sm">{t("transactions.monthView")}</button>}</div>
      </div>
      {isSearch && isFilterOpen && <section aria-label={t("transactions.searchFilters")} className="rounded-xl border border-zinc-200 bg-white p-4">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm">{t("transactions.startDate")}<input aria-label={t("transactions.startDate")} type="date" value={draft.startDate} onChange={(e) => change("startDate", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">{t("transactions.endDate")}<input aria-label={t("transactions.endDate")} type="date" value={draft.endDate} onChange={(e) => change("endDate", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">{t("transactions.type")}<select aria-label={t("transactions.type")} value={draft.transactionType} onChange={(e) => change("transactionType", e.target.value as TransactionFilters["transactionType"])} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">{t("transactions.all")}</option><option value="INCOME">{t("transactions.income")}</option><option value="EXPENSE">{t("transactions.expense")}</option></select></label>
          <label className="text-sm">{t("transactions.category")}<select aria-label={t("transactions.category")} value={draft.categoryUid} onChange={(e) => change("categoryUid", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">{t("transactions.all")}</option>{options.categories.map((item) => <option key={item.categoryUid} value={item.categoryUid}>{item.name}</option>)}</select></label>
          <label className="text-sm">{t("transactions.account")}<select aria-label={t("transactions.account")} value={draft.accountUid} onChange={(e) => change("accountUid", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="">{t("transactions.all")}</option>{options.accounts.map((item) => <option key={item.accountUid} value={item.accountUid}>{item.name}</option>)}</select></label>
          <label className="text-sm">{t("transactions.keyword")}<input aria-label={t("transactions.keyword")} maxLength={200} value={draft.keyword} onChange={(e) => change("keyword", e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") applySearch(); }} className="mt-1 min-h-11 w-full rounded-lg border px-3" placeholder={t("transactions.keywordHint")} /></label>
          <label className="text-sm">{t("transactions.minAmount")}<input aria-label={t("transactions.minAmount")} type="number" min="0" step="0.01" value={draft.minAmount} onChange={(e) => change("minAmount", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">{t("transactions.maxAmount")}<input aria-label={t("transactions.maxAmount")} type="number" min="0" step="0.01" value={draft.maxAmount} onChange={(e) => change("maxAmount", e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border px-3" /></label>
          <label className="text-sm">{t("transactions.sort")}<select aria-label={t("transactions.sort")} value={draft.sort} onChange={(e) => change("sort", e.target.value as TransactionFilters["sort"])} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value="DATE_DESC">{t("transactions.newest")}</option><option value="DATE_ASC">{t("transactions.oldest")}</option><option value="AMOUNT_DESC">{t("transactions.amountHigh")}</option><option value="AMOUNT_ASC">{t("transactions.amountLow")}</option></select></label>
          <label className="text-sm">{t("transactions.pageSize")}<select aria-label={t("transactions.pageSize")} value={draft.size} onChange={(e) => change("size", Number(e.target.value))} className="mt-1 min-h-11 w-full rounded-lg border px-3"><option value={20}>20</option><option value={50}>50</option><option value={100}>100</option></select></label>
        </div>
        {!isValidSearchDateRange(draft.startDate, draft.endDate) && <p role="alert" className="mt-3 text-sm text-red-700">{t("transactions.dateRangeError")}</p>}
        {!isValidSearchAmount(draft.minAmount) || !isValidSearchAmount(draft.maxAmount) || draft.minAmount && draft.maxAmount && Number(draft.minAmount) > Number(draft.maxAmount)
          ? <p role="alert" className="mt-3 text-sm text-red-700">{t("transactions.amountRangeError")}</p> : null}
        <div className="mt-4 flex flex-wrap gap-2"><button type="button" onClick={applySearch} disabled={!isValidSearchDateRange(draft.startDate, draft.endDate) || !isValidSearchAmount(draft.minAmount) || !isValidSearchAmount(draft.maxAmount) || Boolean(draft.minAmount && draft.maxAmount && Number(draft.minAmount) > Number(draft.maxAmount))} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:opacity-50">{t("transactions.apply")}</button><button type="button" onClick={resetSearch} className="min-h-11 rounded-lg border px-4 text-sm">{t("transactions.reset")}</button></div>
      </section>}
      {shownLoading ? <p role="status">{t("transactions.loading")}</p> :
        shownError ? <p role="alert" className="text-red-600">{shownErrorMessage}</p> :
        shownTransactions.length === 0 ? <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-center">
          <p>{t(isSearch ? "transactions.noSearchResults" : "transactions.noMonthEntries")}</p>
          {!isSearch && canCreate && <button type="button" onClick={() => setCreateOpen(true)}
            className="mt-4 min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">{t("transactions.add")}</button>}
        </div> : <ul className="space-y-3">
          {shownTransactions.map((transaction) => <TransactionRow key={transaction.transactionUid} transaction={transaction}
            onSelect={() => setSelectedTransactionUid(transaction.transactionUid)} />)}
        </ul>}
      {isSearch && search.result && search.result.totalPages > 1 && <nav aria-label={t("transactions.searchFilters")} className="flex items-center justify-center gap-3">
        <button type="button" aria-label={t("transactions.previousPage")} disabled={search.result.first} onClick={() => search.update({ ...search.filters, page: Math.max(0, search.filters.page - 1) })} className="min-h-11 rounded-lg border px-4 disabled:opacity-50">{t("transactions.previous")}</button>
        <span aria-live="polite">{search.result.totalPages === 0 ? 0 : search.result.page + 1} / {search.result.totalPages}</span>
        <button type="button" aria-label={t("transactions.nextPage")} disabled={search.result.last} onClick={() => search.update({ ...search.filters, page: search.filters.page + 1 })} className="min-h-11 rounded-lg border px-4 disabled:opacity-50">{t("transactions.next")}</button>
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
