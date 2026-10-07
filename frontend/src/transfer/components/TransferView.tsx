"use client";

import { useState } from "react";
import { formatMoney } from "@/common/format/money";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthNavigation } from "@/transaction/hooks/useMonthNavigation";
import MonthSelector from "@/transaction/components/MonthSelector";
import { useMonthlyTransfers } from "../hooks/useMonthlyTransfers";
import TransferFormDialog from "./TransferFormDialog";
import TransferDetailDialog from "./TransferDetailDialog";

export default function TransferView({ moneyBookUid }: { moneyBookUid: number }) {
  const [isCreating, setCreating] = useState(false);
  const [selectedUid, setSelectedUid] = useState<number | null>(null);
  const { canRead, canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { year, month, moveMonth, goToToday } = useMonthNavigation();
  const { transfers, isLoading, isError, errorMessage } = useMonthlyTransfers(moneyBookUid, year, month, canRead);
  function changeMonth(offset: -1 | 1) { setSelectedUid(null); moveMonth(offset); }

  return <div className="space-y-5">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div><h1 className="text-2xl font-semibold">계좌 간 이체</h1><p className="mt-1 text-sm text-zinc-600">출금 계좌에서 입금 계좌로 이동한 내역입니다.</p></div>
      {canCreate && <button type="button" onClick={() => setCreating(true)} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">이체 추가</button>}
    </div>
    <div className="flex flex-wrap items-center gap-2">
      <MonthSelector year={year} month={month} onPrevious={() => changeMonth(-1)} onNext={() => changeMonth(1)} />
      <button type="button" onClick={goToToday} className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm">이번 달</button>
    </div>
    {!canRead ? <p role="alert">이체 조회 권한이 없습니다.</p> : isLoading ? <p role="status">이체를 불러오는 중...</p> :
      isError ? <p role="alert" className="text-red-600">{errorMessage}</p> : transfers.length === 0 ?
        <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-center">
          <p>이 달에 등록된 이체가 없습니다.</p>
          {canCreate && <button type="button" onClick={() => setCreating(true)} className="mt-4 min-h-11 rounded-lg bg-blue-600 px-4 text-sm text-white">이체 추가</button>}
        </div> : <ul className="space-y-3">
          {transfers.map((transfer) => <li key={transfer.transferUid}>
            <button type="button" onClick={() => setSelectedUid(transfer.transferUid)} className="w-full rounded-xl border border-zinc-200 bg-white p-4 text-left hover:border-blue-300 sm:flex sm:items-center sm:justify-between sm:gap-4">
              <span className="block text-xs text-zinc-500">{transfer.transferDate} · 이체</span>
              <span className="mt-2 block min-w-0 flex-1 text-sm font-medium sm:mt-0">{transfer.fromAccountName} <span aria-hidden="true">→</span> {transfer.toAccountName}</span>
              <span className="mt-2 block font-semibold sm:mt-0">{formatMoney(transfer.amount)}</span>
              {transfer.memo && <span className="mt-2 block break-words text-xs text-zinc-600 sm:mt-0 sm:max-w-36 sm:truncate">{transfer.memo}</span>}
            </button>
          </li>)}
        </ul>}
    {isCreating && <TransferFormDialog moneyBookUid={moneyBookUid} onClose={() => setCreating(false)} onSaved={() => setCreating(false)} />}
    {selectedUid !== null && <TransferDetailDialog moneyBookUid={moneyBookUid} transferUid={selectedUid}
      canUpdate={canUpdate} canDelete={canDelete} onClose={() => setSelectedUid(null)} />}
  </div>;
}
