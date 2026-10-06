"use client";

import { useState } from "react";
import DialogShell from "@/common/components/DialogShell";
import { formatMoney } from "@/common/format/money";
import { useTransferDetail } from "../hooks/useTransferDetail";
import { useDeleteTransfer } from "../hooks/useDeleteTransfer";
import TransferFormDialog from "./TransferFormDialog";

export default function TransferDetailDialog({ moneyBookUid, transferUid, canUpdate, canDelete, onClose }: {
  moneyBookUid: number; transferUid: number; canUpdate: boolean; canDelete: boolean; onClose: () => void;
}) {
  const [isEditing, setEditing] = useState(false);
  const { transfer, isLoading, isError, errorMessage } = useTransferDetail(moneyBookUid, transferUid);
  const deletion = useDeleteTransfer(moneyBookUid);
  if (isEditing && transfer) return <TransferFormDialog moneyBookUid={moneyBookUid} initial={transfer}
    onClose={() => setEditing(false)} onSaved={onClose} />;

  return <DialogShell title="이체 상세" onClose={onClose}>
    {isLoading ? <p role="status">이체를 불러오는 중...</p> : isError ?
      <p role="alert" className="text-red-600">{errorMessage}</p> : !transfer ?
        <p role="alert">이체를 찾을 수 없습니다.</p> : <div className="space-y-4">
          <strong className="block border-b border-zinc-200 pb-4 text-xl">이체 {formatMoney(transfer.amount)}</strong>
          <dl className="grid grid-cols-[5rem_1fr] gap-3 text-sm">
            <dt className="text-zinc-600">날짜</dt><dd>{transfer.transferDate}</dd>
            <dt className="text-zinc-600">출금</dt><dd>{transfer.fromAccountName}</dd>
            <dt className="text-zinc-600">입금</dt><dd>{transfer.toAccountName}</dd>
            <dt className="text-zinc-600">메모</dt><dd className="whitespace-pre-wrap break-words">{transfer.memo || "없음"}</dd>
          </dl>
          {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
          {(canUpdate || canDelete) && <div className="flex gap-2">
            {canUpdate && <button type="button" onClick={() => setEditing(true)} className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-4">수정</button>}
            {canDelete && <button type="button" disabled={deletion.isLoading} onClick={async () => { if (await deletion.remove(transferUid)) onClose(); }}
              className="min-h-11 flex-1 rounded-lg border border-red-200 px-4 text-red-700 disabled:opacity-50">{deletion.isLoading ? "삭제 중..." : "삭제"}</button>}
          </div>}
        </div>}
  </DialogShell>;
}
