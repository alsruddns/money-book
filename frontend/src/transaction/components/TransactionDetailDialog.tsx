"use client";

import { useState } from "react";
import DialogShell from "@/common/components/DialogShell";
import { useTransactionDetail } from "../hooks/useTransactionDetail";
import { useDeleteTransaction } from "../hooks/useDeleteTransaction";
import { formatWon } from "../transactionForm";
import TransactionFormDialog from "./TransactionFormDialog";
import { formatLocalDate } from "@/common/format/dateTime";

export default function TransactionDetailDialog({ moneyBookUid, transactionUid, canUpdate, canDelete, onClose }: {
  moneyBookUid: number; transactionUid: number; canUpdate: boolean; canDelete: boolean; onClose: () => void;
}) {
  const [isEditing, setEditing] = useState(false);
  const { transaction, isLoading, isError, errorMessage } = useTransactionDetail(moneyBookUid, transactionUid);
  const deletion = useDeleteTransaction(moneyBookUid);

  async function handleDelete() {
    if (await deletion.deleteTransaction(transactionUid)) onClose();
  }

  if (isEditing && transaction) return <TransactionFormDialog moneyBookUid={moneyBookUid} initial={transaction}
    onClose={() => setEditing(false)} onSaved={onClose} />;

  return (
    <DialogShell title="거래 상세" onClose={onClose}>
      {isLoading ? <p role="status">거래를 불러오는 중...</p> :
        isError ? <p role="alert" className="text-red-600">{errorMessage}</p> :
        !transaction ? <p role="alert">거래를 찾을 수 없습니다.</p> : <div className="space-y-4">
          <div className="flex items-center justify-between gap-3 border-b border-zinc-200 pb-4">
            <span className="font-medium">{transaction.transactionType === "INCOME" ? "수입" : "지출"}</span>
            <strong className="text-xl">{formatWon(transaction.amount)}</strong>
          </div>
          <dl className="grid grid-cols-[5rem_1fr] gap-x-3 gap-y-3 text-sm">
            <dt className="text-zinc-600">날짜</dt><dd>{formatLocalDate(transaction.transactionDate)}</dd>
            <dt className="text-zinc-600">카테고리</dt><dd>{transaction.categoryName}</dd>
            <dt className="text-zinc-600">계좌</dt><dd>{transaction.accountName}</dd>
            <dt className="text-zinc-600">메모</dt><dd className="whitespace-pre-wrap break-words">{transaction.memo || "없음"}</dd>
          </dl>
          {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
          {(canUpdate || canDelete) && <div className="flex gap-2 pt-2">
            {canUpdate && <button type="button" onClick={() => setEditing(true)}
              className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-4 text-sm font-medium">수정</button>}
            {canDelete && <button type="button" disabled={deletion.isLoading} onClick={() => void handleDelete()}
              className="min-h-11 flex-1 rounded-lg border border-red-200 px-4 text-sm font-medium text-red-700 disabled:opacity-60">
              {deletion.isLoading ? "삭제 중..." : "삭제"}
            </button>}
          </div>}
        </div>}
    </DialogShell>
  );
}
