"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import DialogShell from "@/common/components/DialogShell";
import type { TransactionType } from "../dto/TransactionType";
import type { TransactionResponse } from "../dto/res/TransactionResponse";
import { useTransactionFormOptions } from "../hooks/useTransactionFormOptions";
import { useCreateTransaction } from "../hooks/useCreateTransaction";
import { useUpdateTransaction } from "../hooks/useUpdateTransaction";
import { todayLocalDate, type TransactionFormValues } from "../transactionForm";

export default function TransactionFormDialog({ moneyBookUid, initial, initialDate, initialType, onClose, onSaved }: {
  moneyBookUid: number; initial?: TransactionResponse; initialDate?: string; initialType?: TransactionType; onClose: () => void; onSaved: () => void;
}) {
  const [values, setValues] = useState<TransactionFormValues>({
    transactionType: initial?.transactionType ?? initialType ?? "EXPENSE",
    amount: initial ? String(initial.amount) : "",
    transactionDate: initial?.transactionDate ?? initialDate ?? todayLocalDate(),
    categoryUid: initial ? String(initial.categoryUid) : "",
    accountUid: initial ? String(initial.accountUid) : "",
    memo: initial?.memo ?? "",
  });
  const options = useTransactionFormOptions(moneyBookUid, values.transactionType);
  const creation = useCreateTransaction(moneyBookUid);
  const update = useUpdateTransaction(moneyBookUid, initial?.transactionUid ?? null);
  const isLoading = initial ? update.isLoading : creation.isLoading;
  const errorMessage = initial ? update.errorMessage : creation.errorMessage;

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
    <DialogShell title={initial ? "거래 수정" : "거래 등록"} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div><label htmlFor="transaction-type" className="mb-1 block text-sm font-medium">유형</label>
          <select id="transaction-type" value={values.transactionType}
            onChange={(event) => setValues((current) => ({ ...current, transactionType: event.target.value as TransactionType, categoryUid: "" }))}
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
            <div><label htmlFor="transaction-category" className="mb-1 block text-sm font-medium">카테고리</label>
              <select id="transaction-category" required disabled={options.categories.length === 0} value={values.categoryUid}
                onChange={(event) => setField("categoryUid", event.target.value)} className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 disabled:opacity-60">
                <option value="">선택해 주세요</option>
                {options.categories.map((category) => <option key={category.categoryUid} value={category.categoryUid}>{category.name}</option>)}
              </select></div>
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
