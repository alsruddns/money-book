"use client";

import { useState } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMonthNavigation } from "../hooks/useMonthNavigation";
import { useMonthlyTransactions } from "../hooks/useMonthlyTransactions";
import MonthSelector from "./MonthSelector";
import TransactionRow from "./TransactionRow";
import TransactionFormDialog from "./TransactionFormDialog";
import TransactionDetailDialog from "./TransactionDetailDialog";

export default function TransactionList({ moneyBookUid }: { moneyBookUid: number }) {
  const [isCreateOpen, setCreateOpen] = useState(false);
  const [selectedTransactionUid, setSelectedTransactionUid] = useState<number | null>(null);
  const { canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { year, month, moveMonth } = useMonthNavigation();
  const { transactions, isLoading, isError, errorMessage } = useMonthlyTransactions(moneyBookUid, year, month);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div><h1 className="text-2xl font-semibold">거래 내역</h1><p className="mt-1 text-sm text-zinc-600">월별 수입과 지출을 확인합니다.</p></div>
        {canCreate && <button type="button" onClick={() => setCreateOpen(true)}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">거래 추가</button>}
      </div>
      <MonthSelector year={year} month={month} onPrevious={() => moveMonth(-1)} onNext={() => moveMonth(1)} />
      {isLoading ? <p role="status">거래를 불러오는 중...</p> :
        isError ? <p role="alert" className="text-red-600">{errorMessage}</p> :
        transactions.length === 0 ? <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-center">
          <p>이 달에 등록된 거래가 없습니다.</p>
          {canCreate && <button type="button" onClick={() => setCreateOpen(true)}
            className="mt-4 min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">거래 추가</button>}
        </div> : <ul className="space-y-3">
          {transactions.map((transaction) => <TransactionRow key={transaction.transactionUid} transaction={transaction}
            onSelect={() => setSelectedTransactionUid(transaction.transactionUid)} />)}
        </ul>}
      {isCreateOpen && <TransactionFormDialog moneyBookUid={moneyBookUid}
        onClose={() => setCreateOpen(false)} onSaved={() => setCreateOpen(false)} />}
      {selectedTransactionUid !== null && <TransactionDetailDialog moneyBookUid={moneyBookUid}
        transactionUid={selectedTransactionUid} canUpdate={canUpdate} canDelete={canDelete}
        onClose={() => setSelectedTransactionUid(null)} />}
    </div>
  );
}
