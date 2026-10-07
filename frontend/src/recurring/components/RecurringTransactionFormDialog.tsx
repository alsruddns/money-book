"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import DialogShell from "@/common/components/DialogShell";
import { todayLocalDate } from "@/transaction/transactionForm";
import { useTransactionFormOptions } from "@/transaction/hooks/useTransactionFormOptions";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { RecurringFrequency } from "../dto/RecurringFrequency";
import { recurringWeekdays } from "../dto/RecurringFrequency";
import type { RecurringTransactionResponse } from "../dto/res/RecurringTransactionResponse";
import type { RecurringFormValues } from "../recurringForm";
import { useCreateRecurringTransaction } from "../hooks/useCreateRecurringTransaction";
import { useUpdateRecurringTransaction } from "../hooks/useUpdateRecurringTransaction";

export default function RecurringTransactionFormDialog({ moneyBookUid, initial, onClose, onSaved }: {
  moneyBookUid: number; initial?: RecurringTransactionResponse; onClose: () => void; onSaved: () => void;
}) {
  const [values, setValues] = useState<RecurringFormValues>({
    transactionType: initial?.transactionType ?? "EXPENSE", amount: initial ? String(initial.amount) : "",
    categoryUid: initial ? String(initial.categoryUid) : "", accountUid: initial ? String(initial.accountUid) : "",
    frequency: initial?.frequency ?? "MONTHLY", dayOfMonth: initial?.dayOfMonth ? String(initial.dayOfMonth) : "",
    dayOfWeek: initial?.dayOfWeek ? String(initial.dayOfWeek) : "", startDate: initial?.startDate ?? todayLocalDate(),
    endDate: initial?.endDate ?? "", memo: initial?.memo ?? "",
  });
  const options = useTransactionFormOptions(moneyBookUid, values.transactionType);
  const creation = useCreateRecurringTransaction(moneyBookUid);
  const update = useUpdateRecurringTransaction(moneyBookUid, initial?.recurringTransactionUid ?? null);
  const isSaving = initial ? update.isLoading : creation.isLoading;
  const errorMessage = initial ? update.errorMessage : creation.errorMessage;

  function setField<K extends keyof RecurringFormValues>(key: K, value: RecurringFormValues[K]) {
    setValues((current) => ({ ...current, [key]: value }));
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving || options.isLoading || options.isError || !options.accounts.length || !options.categories.length) return;
    const saved = initial ? await update.update(values) : await creation.create(values);
    if (saved) onSaved();
  }

  return <DialogShell title={initial ? "정기 수입/지출 수정" : "정기 수입/지출 등록"} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      <label className="block text-sm font-medium">유형
        <select value={values.transactionType} onChange={(event) => setValues((current) => ({ ...current,
          transactionType: event.target.value as TransactionType, categoryUid: "",
        }))} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
          <option value="EXPENSE">지출</option><option value="INCOME">수입</option>
        </select>
      </label>
      <label className="block text-sm font-medium">금액
        <input required type="text" inputMode="decimal" value={values.amount} onChange={(event) => setField("amount", event.target.value)}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      {options.isLoading ? <p role="status">카테고리와 계좌를 불러오는 중...</p> : options.isError ?
        <p role="alert" className="text-red-600">{options.errorMessage}</p> : <>
          {options.categories.length === 0 && <p className="text-sm text-amber-800">해당 유형의 카테고리가 없습니다. <Link href={`/books/${moneyBookUid}/categories`} className="underline">카테고리 관리</Link></p>}
          <label className="block text-sm font-medium">카테고리
            <select required disabled={!options.categories.length} value={values.categoryUid} onChange={(event) => setField("categoryUid", event.target.value)}
              className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
              <option value="">선택해 주세요</option>{options.categories.map((category) => <option key={category.categoryUid} value={category.categoryUid}>{category.name}</option>)}
            </select>
          </label>
          {options.accounts.length === 0 && <p className="text-sm text-amber-800">계좌/결제수단이 없습니다. <Link href={`/books/${moneyBookUid}/accounts`} className="underline">계좌 관리</Link></p>}
          <label className="block text-sm font-medium">계좌/결제수단
            <select required disabled={!options.accounts.length} value={values.accountUid} onChange={(event) => setField("accountUid", event.target.value)}
              className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
              <option value="">선택해 주세요</option>{options.accounts.map((account) => <option key={account.accountUid} value={account.accountUid}>{account.name}</option>)}
            </select>
          </label>
        </>}
      <label className="block text-sm font-medium">반복 주기
        <select value={values.frequency} onChange={(event) => setValues((current) => ({ ...current,
          frequency: event.target.value as RecurringFrequency, dayOfMonth: "", dayOfWeek: "",
        }))} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
          <option value="MONTHLY">매월</option><option value="WEEKLY">매주</option>
        </select>
      </label>
      {values.frequency === "MONTHLY" ? <label className="block text-sm font-medium">매월 반복일
        <input required type="number" min={1} max={31} value={values.dayOfMonth} onChange={(event) => setField("dayOfMonth", event.target.value)}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
        <span className="mt-1 block text-xs font-normal text-zinc-600">29~31일로 설정하면 해당 날짜가 없는 달에는 그 달의 마지막 날에 처리됩니다.</span>
      </label> : <label className="block text-sm font-medium">매주 반복 요일
        <select required value={values.dayOfWeek} onChange={(event) => setField("dayOfWeek", event.target.value)}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
          <option value="">선택해 주세요</option>{recurringWeekdays.map((day) => <option key={day.value} value={day.value}>{day.label}</option>)}
        </select>
      </label>}
      <label className="block text-sm font-medium">시작 날짜
        <input required type="date" value={values.startDate} onChange={(event) => setField("startDate", event.target.value)}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      <label className="block text-sm font-medium">종료 날짜 (선택)
        <input type="date" value={values.endDate} onChange={(event) => setField("endDate", event.target.value)}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      <label className="block text-sm font-medium">메모 (선택)
        <textarea maxLength={500} rows={3} value={values.memo} onChange={(event) => setField("memo", event.target.value)}
          className="mt-1 w-full rounded-lg border border-zinc-300 px-3 py-2" />
      </label>
      {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
      <div className="flex gap-2">
        <button type="button" onClick={onClose} className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-4">취소</button>
        <button type="submit" disabled={isSaving || options.isLoading || options.isError || !options.accounts.length || !options.categories.length}
          className="min-h-11 flex-1 rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-50">{isSaving ? "저장 중..." : "저장"}</button>
      </div>
    </form>
  </DialogShell>;
}
