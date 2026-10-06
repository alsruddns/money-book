"use client";

import { useState, type FormEvent } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { todayLocalDate } from "@/transaction/transactionForm";
import type { RecurringTransactionResponse } from "../dto/res/RecurringTransactionResponse";
import { useRecurringTransactions } from "../hooks/useRecurringTransactions";
import { useToggleRecurringTransaction } from "../hooks/useToggleRecurringTransaction";
import { useDeleteRecurringTransaction } from "../hooks/useDeleteRecurringTransaction";
import { useGenerateRecurringTransactions } from "../hooks/useGenerateRecurringTransactions";
import RecurringTransactionCard from "./RecurringTransactionCard";
import RecurringTransactionFormDialog from "./RecurringTransactionFormDialog";

export default function RecurringTransactionView({ moneyBookUid }: { moneyBookUid: number }) {
  const [isCreating, setCreating] = useState(false);
  const [editing, setEditing] = useState<RecurringTransactionResponse | null>(null);
  const [baseDate, setBaseDate] = useState(todayLocalDate());
  const { canRead, canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { rules, isLoading, isFetching, isError, errorMessage } = useRecurringTransactions(moneyBookUid, canRead);
  const activation = useToggleRecurringTransaction(moneyBookUid);
  const deletion = useDeleteRecurringTransaction(moneyBookUid);
  const generation = useGenerateRecurringTransactions(moneyBookUid);

  async function submitGenerate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await generation.generate(baseDate);
  }

  return <div className="space-y-6">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><h1 className="text-2xl font-semibold">정기 수입/지출</h1><p className="mt-1 text-sm text-zinc-600">반복 규칙을 관리하고 도래한 거래를 직접 반영합니다.</p></div>
      {canCreate && <button type="button" onClick={() => setCreating(true)} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">정기 수입/지출 추가</button>}
    </div>
    {canCreate && <form onSubmit={submitGenerate} className="rounded-xl border border-zinc-200 bg-white p-4 sm:flex sm:items-end sm:gap-3">
      <label className="block flex-1 text-sm font-medium">정기 거래 반영 기준일
        <input required type="date" value={baseDate} onChange={(event) => setBaseDate(event.target.value)} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      <button type="submit" disabled={generation.isLoading} className="mt-3 min-h-11 rounded-lg border border-blue-600 px-4 text-sm font-medium text-blue-700 disabled:opacity-50 sm:mt-0">
        {generation.isLoading ? "반영 중..." : "기준일까지 정기 거래 반영"}
      </button>
    </form>}
    {generation.errorMessage && <p role="alert" className="text-sm text-red-600">{generation.errorMessage}</p>}
    {generation.feedback && <p role="status" className="rounded-lg bg-blue-50 p-3 text-sm text-blue-800">{generation.feedback}</p>}
    {activation.errorMessage && <p role="alert" className="text-sm text-red-600">{activation.errorMessage}</p>}
    {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
    {!canRead ? <p role="alert">정기 수입/지출 조회 권한이 없습니다.</p> : isLoading ? <p role="status">정기 규칙을 불러오는 중...</p> :
      isError ? <p role="alert" className="text-red-600">{errorMessage}</p> : <>
        {isFetching && <p role="status" className="text-sm text-zinc-600">정기 규칙을 새로고침하는 중...</p>}
        {rules.length === 0 ? <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-center">
          <p>등록된 정기 수입/지출이 없습니다.</p>
          {canCreate && <button type="button" onClick={() => setCreating(true)} className="mt-4 min-h-11 rounded-lg bg-blue-600 px-4 text-sm text-white">정기 수입/지출 추가</button>}
        </div> : <div className="grid gap-3 xl:grid-cols-2">
          {rules.map((rule) => <RecurringTransactionCard key={rule.recurringTransactionUid} rule={rule}
            canUpdate={canUpdate} canDelete={canDelete} busy={activation.isLoading || deletion.isLoading}
            onEdit={() => setEditing(rule)}
            onToggle={() => void activation.toggle(rule.recurringTransactionUid, !rule.isActive)}
            onDelete={() => void deletion.remove(rule.recurringTransactionUid)} />)}
        </div>}
      </>}
    {isCreating && <RecurringTransactionFormDialog moneyBookUid={moneyBookUid} onClose={() => setCreating(false)} onSaved={() => setCreating(false)} />}
    {editing && <RecurringTransactionFormDialog moneyBookUid={moneyBookUid} initial={editing} onClose={() => setEditing(null)} onSaved={() => setEditing(null)} />}
  </div>;
}
