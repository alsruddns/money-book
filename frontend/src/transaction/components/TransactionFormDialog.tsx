"use client";

import { useState, type FormEvent } from "react";
import Link from "../../common/components/MoneyLink";
import DialogShell from "@/common/components/DialogShell";
import type { TransactionType } from "../dto/TransactionType";
import type { TransactionResponse } from "../dto/res/TransactionResponse";
import { useTransactionFormOptions } from "../hooks/useTransactionFormOptions";
import { useCreateTransaction } from "../hooks/useCreateTransaction";
import { useUpdateTransaction } from "../hooks/useUpdateTransaction";
import { selectLargeCategory, selectMediumCategory, todayLocalDate, type TransactionFormValues } from "../transactionForm";
import { useTranslation } from "@/i18n/useTranslation";

export default function TransactionFormDialog({ moneyBookUid, initial, initialDate, initialType, onClose, onSaved }: {
  moneyBookUid: number; initial?: TransactionResponse; initialDate?: string; initialType?: TransactionType; onClose: () => void; onSaved: () => void;
}) {
  const [values, setValues] = useState<TransactionFormValues>({
    transactionType: initial?.transactionType ?? initialType ?? "EXPENSE",
    amount: initial ? String(initial.amount) : "",
    transactionDate: initial?.transactionDate ?? initialDate ?? todayLocalDate(),
    largeCategoryUid: initial ? String(initial.categoryPath?.largeCategory?.categoryUid ?? initial.categoryUid) : "",
    mediumCategoryUid: initial?.categoryPath?.mediumCategory ? String(initial.categoryPath.mediumCategory.categoryUid) : "",
    smallCategoryUid: initial?.categoryPath?.smallCategory ? String(initial.categoryPath.smallCategory.categoryUid) : "",
    accountUid: initial ? String(initial.accountUid) : "",
    memo: initial?.memo ?? "",
  });
  const options = useTransactionFormOptions(moneyBookUid, values.transactionType);
  const creation = useCreateTransaction(moneyBookUid);
  const update = useUpdateTransaction(moneyBookUid, initial?.transactionUid ?? null);
  const isLoading = initial ? update.isLoading : creation.isLoading;
  const errorMessage = initial ? update.errorMessage : creation.errorMessage;
  const { t } = useTranslation();
  const largeOptions = options.categories.filter((category) => category.level === "LARGE");
  const selectedLarge = options.categories.find((category) => category.categoryUid === Number(values.largeCategoryUid));
  const mediumOptions = selectedLarge?.children.filter((category) => category.level === "MEDIUM") ?? [];
  const selectedMedium = mediumOptions.find((category) => category.categoryUid === Number(values.mediumCategoryUid));
  const smallOptions = selectedMedium?.children.filter((category) => category.level === "SMALL") ?? [];

  function setField<K extends keyof TransactionFormValues>(key: K, value: TransactionFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading || options.isLoading || options.isError) return;
    const saved = initial ? await update.updateTransaction(values) : await creation.createTransaction(values);
    if (saved) onSaved();
  }

  return (
      <DialogShell title={initial ? t("transactions.edit") : t("transactions.add")} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div><label htmlFor="transaction-type" className="mb-1 block text-sm font-medium">유형</label>
          <select id="transaction-type" value={values.transactionType}
            onChange={(event) => setValues((current) => ({ ...current, transactionType: event.target.value as TransactionType,
              largeCategoryUid: "", mediumCategoryUid: "", smallCategoryUid: "" }))}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5">
            <option value="EXPENSE">지출</option><option value="INCOME">수입</option>
          </select></div>
        <div><label htmlFor="transaction-amount" className="mb-1 block text-sm font-medium">금액</label>
          <input id="transaction-amount" type="number" min="0.01" step="0.01" required inputMode="decimal" value={values.amount}
            onChange={(event) => setField("amount", event.target.value)} className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" /></div>
        <div><label htmlFor="transaction-date" className="mb-1 block text-sm font-medium">날짜</label>
          <input id="transaction-date" type="date" required value={values.transactionDate}
            onChange={(event) => setField("transactionDate", event.target.value)} className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" /></div>
        {options.isLoading ? <p role="status" className="text-sm">선택 항목을 불러오는 중...</p> :
          options.isError ? <p role="alert" className="text-sm text-red-600">{options.errorMessage}</p> : <>
            {options.categories.length === 0 && <p className="text-sm text-amber-800">
              먼저 {values.transactionType === "INCOME" ? "수입" : "지출"} 카테고리를 등록해 주세요. <Link href={`/books/${moneyBookUid}/categories`} className="font-medium underline">카테고리 관리</Link>
            </p>}
            <div className="grid gap-4 md:grid-cols-3">
              <label htmlFor="large-category" className="block text-sm font-medium">{t("category.large")} *
                <select id="large-category" required value={values.largeCategoryUid}
                  onChange={(event) => setValues((current) => selectLargeCategory(current, event.target.value))}
                  className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5">
                  <option value="">{t("category.selectLarge")}</option>
                  {largeOptions.map((category) => <option key={category.categoryUid} value={category.categoryUid}>{category.name}</option>)}
                </select>
              </label>
              <label htmlFor="medium-category" className="block text-sm font-medium">{t("category.medium")}
                <select id="medium-category" disabled={!selectedLarge || mediumOptions.length === 0} value={values.mediumCategoryUid}
                  onChange={(event) => setValues((current) => selectMediumCategory(current, event.target.value))}
                  className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 disabled:opacity-60">
                  <option value="">{selectedLarge && mediumOptions.length === 0 ? t("category.noMedium") : t("category.selectMedium")}</option>
                  {mediumOptions.map((category) => <option key={category.categoryUid} value={category.categoryUid}>{category.name}</option>)}
                </select>
              </label>
              <label htmlFor="small-category" className="block text-sm font-medium">{t("category.small")}
                <select id="small-category" disabled={!selectedMedium || smallOptions.length === 0} value={values.smallCategoryUid}
                  onChange={(event) => setField("smallCategoryUid", event.target.value)}
                  className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 disabled:opacity-60">
                  <option value="">{selectedMedium && smallOptions.length === 0 ? t("category.noSmall") : t("category.selectSmall")}</option>
                  {smallOptions.map((category) => <option key={category.categoryUid} value={category.categoryUid}>{category.name}</option>)}
                </select>
              </label>
            </div>
            {options.accounts.length === 0 && <p className="text-sm text-amber-800">
              먼저 계좌/결제수단을 등록해 주세요. <Link href={`/books/${moneyBookUid}/accounts`} className="font-medium underline">계좌 관리</Link>
            </p>}
            <div><label htmlFor="transaction-account" className="mb-1 block text-sm font-medium">계좌/결제수단</label>
              <select id="transaction-account" required disabled={options.accounts.length === 0} value={values.accountUid}
                onChange={(event) => setField("accountUid", event.target.value)} className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 disabled:opacity-60">
                <option value="">선택해 주세요</option>
                {options.accounts.map((account) => <option key={account.accountUid} value={account.accountUid}>{account.name}</option>)}
              </select></div>
          </>}
        <div><label htmlFor="transaction-memo" className="mb-1 block text-sm font-medium">메모 (선택)</label>
          <textarea id="transaction-memo" maxLength={500} rows={3} value={values.memo}
            onChange={(event) => setField("memo", event.target.value)} className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" /></div>
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading || options.isLoading || options.isError || !options.categories.length || !options.accounts.length}
          className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? "저장 중..." : "저장"}
        </button>
      </form>
    </DialogShell>
  );
}
