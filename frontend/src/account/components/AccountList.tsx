"use client";

import { useState } from "react";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { accountTypeLabels } from "../accountTypes";
import type { AccountResponse } from "../dto/res/AccountResponse";
import { useAccountList } from "../hooks/useAccountList";
import { useDeleteAccount } from "../hooks/useDeleteAccount";
import AccountFormDialog from "./AccountFormDialog";

export default function AccountList({ moneyBookUid }: { moneyBookUid: number }) {
  const [dialog, setDialog] = useState<{ initial?: AccountResponse } | null>(null);
  const { canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { accounts, isLoading, isError, errorMessage } = useAccountList(moneyBookUid);
  const deletion = useDeleteAccount(moneyBookUid);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-2xl font-semibold">계좌/결제수단</h1><p className="mt-1 text-sm text-zinc-600">거래에 사용할 결제수단을 관리합니다.</p></div>
        {canCreate && <button type="button" onClick={() => setDialog({})}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">계좌 추가</button>}
      </div>
      {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
      {isLoading ? <p role="status">계좌를 불러오는 중...</p> : isError
        ? <p role="alert" className="text-red-600">{errorMessage}</p>
        : accounts.length === 0 ? <p className="rounded-xl border border-dashed border-zinc-300 bg-white p-6 text-sm text-zinc-600">등록된 계좌/결제수단이 없습니다.</p>
        : <div className="grid gap-3 sm:grid-cols-2">
          {accounts.map((account) => <article key={account.accountUid} className="rounded-xl border border-zinc-200 bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div><h2 className="font-medium">{account.name}</h2>
                <p className="mt-1 text-sm text-zinc-600">{accountTypeLabels[account.accountType]} · 정렬 순서 {account.sortOrder}</p></div>
              {(canUpdate || canDelete) && <div className="flex gap-2">
                {canUpdate && <button type="button" onClick={() => setDialog({ initial: account })}
                  className="min-h-11 rounded-lg border border-zinc-300 px-3 text-sm">수정</button>}
                {canDelete && <button type="button" disabled={deletion.isLoading}
                  onClick={() => void deletion.deleteAccount(account.accountUid, account.name)}
                  className="min-h-11 rounded-lg border border-red-200 px-3 text-sm text-red-700 disabled:opacity-60">삭제</button>}
              </div>}
            </div>
          </article>)}
        </div>}
      {dialog && <AccountFormDialog moneyBookUid={moneyBookUid} initial={dialog.initial} onClose={() => setDialog(null)} />}
    </div>
  );
}
