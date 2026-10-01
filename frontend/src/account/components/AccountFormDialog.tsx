"use client";

import { useState, type FormEvent } from "react";
import DialogShell from "@/common/components/DialogShell";
import { accountTypeLabels, accountTypes } from "../accountTypes";
import type { AccountType } from "../dto/AccountType";
import type { AccountResponse } from "../dto/res/AccountResponse";
import { useCreateAccount } from "../hooks/useCreateAccount";
import { useUpdateAccount } from "../hooks/useUpdateAccount";

export default function AccountFormDialog({ moneyBookUid, initial, onClose }: {
  moneyBookUid: number; initial?: AccountResponse; onClose: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [accountType, setAccountType] = useState<AccountType>(initial?.accountType ?? "CASH");
  const [sortOrder, setSortOrder] = useState(String(initial?.sortOrder ?? 0));
  const create = useCreateAccount(moneyBookUid);
  const update = useUpdateAccount(moneyBookUid, initial?.accountUid ?? 0);
  const isLoading = initial ? update.isLoading : create.isLoading;
  const errorMessage = initial ? update.errorMessage : create.errorMessage;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading) return;
    const request = { name, accountType, sortOrder: sortOrder.trim() === "" ? Number.NaN : Number(sortOrder) };
    const saved = initial ? await update.updateAccount(request) : await create.createAccount(request);
    if (saved) onClose();
  }

  return (
    <DialogShell title={initial ? "계좌 수정" : "계좌/결제수단 추가"} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div><label htmlFor="account-name" className="mb-1 block text-sm font-medium">이름</label>
          <input id="account-name" required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} autoFocus
            className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" /></div>
        <div><label htmlFor="account-type" className="mb-1 block text-sm font-medium">유형</label>
          <select id="account-type" value={accountType} onChange={(event) => setAccountType(event.target.value as AccountType)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5">
            {accountTypes.map((type) => <option key={type} value={type}>{accountTypeLabels[type]}</option>)}
          </select></div>
        <div><label htmlFor="account-order" className="mb-1 block text-sm font-medium">정렬 순서</label>
          <input id="account-order" type="number" min="0" step="1" required value={sortOrder}
            onChange={(event) => setSortOrder(event.target.value)} className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" /></div>
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? "저장 중..." : "저장"}
        </button>
      </form>
    </DialogShell>
  );
}
