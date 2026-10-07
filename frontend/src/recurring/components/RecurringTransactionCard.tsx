import { formatMoney } from "@/common/format/money";
import { recurringSchedule } from "../dto/RecurringFrequency";
import type { RecurringTransactionResponse } from "../dto/res/RecurringTransactionResponse";

export default function RecurringTransactionCard({ rule, canUpdate, canDelete, busy, onEdit, onToggle, onDelete }: {
  rule: RecurringTransactionResponse; canUpdate: boolean; canDelete: boolean; busy: boolean;
  onEdit: () => void; onToggle: () => void; onDelete: () => void;
}) {
  return <article className="min-w-0 space-y-3 rounded-xl border border-zinc-200 bg-white p-4">
    <div className="flex flex-wrap items-start justify-between gap-2">
      <div><p className="text-sm font-medium">{rule.transactionType === "INCOME" ? "수입" : "지출"} · {recurringSchedule(rule.frequency, rule.dayOfMonth, rule.dayOfWeek)}</p>
        <strong className="mt-1 block text-xl">{formatMoney(rule.amount)}</strong></div>
      <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${rule.isActive ? "bg-green-100 text-green-800" : "bg-zinc-100 text-zinc-600"}`}>{rule.isActive ? "활성" : "비활성"}</span>
    </div>
    <dl className="grid grid-cols-[5rem_1fr] gap-x-2 gap-y-1 text-sm">
      <dt className="text-zinc-600">카테고리</dt><dd className="break-words">{rule.categoryName}</dd>
      <dt className="text-zinc-600">계좌</dt><dd className="break-words">{rule.accountName}</dd>
      <dt className="text-zinc-600">시작</dt><dd>{rule.startDate}</dd>
      <dt className="text-zinc-600">종료</dt><dd>{rule.endDate ?? "없음"}</dd>
      <dt className="text-zinc-600">마지막 반영</dt><dd>{rule.lastGeneratedDate ?? "없음"}</dd>
      {rule.memo && <><dt className="text-zinc-600">메모</dt><dd className="whitespace-pre-wrap break-words">{rule.memo}</dd></>}
    </dl>
    {(canUpdate || canDelete) && <div className="flex flex-wrap gap-2 border-t border-zinc-100 pt-3">
      {canUpdate && <><button type="button" onClick={onEdit} disabled={busy} className="min-h-11 rounded-lg border border-zinc-300 px-4 text-sm disabled:opacity-50">수정</button>
        <button type="button" onClick={onToggle} disabled={busy} className="min-h-11 rounded-lg border border-zinc-300 px-4 text-sm disabled:opacity-50">{rule.isActive ? "비활성화" : "활성화"}</button></>}
      {canDelete && <button type="button" onClick={onDelete} disabled={busy} className="min-h-11 rounded-lg border border-red-200 px-4 text-sm text-red-700 disabled:opacity-50">삭제</button>}
    </div>}
  </article>;
}
