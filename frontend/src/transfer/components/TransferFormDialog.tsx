"use client";

import { useState, type FormEvent } from "react";
import Link from "../../common/components/MoneyLink";
import DialogShell from "@/common/components/DialogShell";
import { useAccountList } from "@/account/hooks/useAccountList";
import { todayLocalDate } from "@/transaction/transactionForm";
import type { TransferResponse } from "../dto/res/TransferResponse";
import { useCreateTransfer } from "../hooks/useCreateTransfer";
import { useUpdateTransfer } from "../hooks/useUpdateTransfer";
import type { TransferFormValues } from "../transferForm";

export default function TransferFormDialog({ moneyBookUid, initial, onClose, onSaved }: {
  moneyBookUid: number; initial?: TransferResponse; onClose: () => void; onSaved: () => void;
}) {
  const [values, setValues] = useState<TransferFormValues>({
    fromAccountUid: initial ? String(initial.fromAccountUid) : "",
    toAccountUid: initial ? String(initial.toAccountUid) : "",
    amount: initial ? String(initial.amount) : "",
    transferDate: initial?.transferDate ?? todayLocalDate(), memo: initial?.memo ?? "",
  });
  const accounts = useAccountList(moneyBookUid);
  const creation = useCreateTransfer(moneyBookUid);
  const update = useUpdateTransfer(moneyBookUid, initial?.transferUid ?? null);
  const isSaving = initial ? update.isLoading : creation.isLoading;
  const errorMessage = initial ? update.errorMessage : creation.errorMessage;
  const sameAccount = values.fromAccountUid !== "" && values.fromAccountUid === values.toAccountUid;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving || accounts.isLoading || accounts.isError || accounts.accounts.length < 2) return;
    const saved = initial ? await update.update(values) : await creation.create(values);
    if (saved) onSaved();
  }

  return <DialogShell title={initial ? "이체 수정" : "이체 등록"} onClose={onClose}>
    <form onSubmit={submit} className="space-y-4">
      {accounts.isLoading ? <p role="status">계좌를 불러오는 중...</p> : accounts.isError ?
        <p role="alert" className="text-red-600">{accounts.errorMessage}</p> : accounts.accounts.length < 2 &&
        <p className="text-sm text-amber-800">이체하려면 계좌/결제수단이 2개 이상 필요합니다. <Link href={`/books/${moneyBookUid}/accounts`} className="underline">계좌/결제수단 관리</Link></p>}
      <label className="block text-sm font-medium">출금 계좌
        <select required value={values.fromAccountUid} onChange={(event) => setValues((current) => ({ ...current, fromAccountUid: event.target.value }))}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
          <option value="">선택해 주세요</option>{accounts.accounts.map((account) => <option key={account.accountUid} value={account.accountUid}>{account.name}</option>)}
        </select>
      </label>
      <p aria-hidden="true" className="text-center text-xl text-zinc-500">↓</p>
      <label className="block text-sm font-medium">입금 계좌
        <select required value={values.toAccountUid} onChange={(event) => setValues((current) => ({ ...current, toAccountUid: event.target.value }))}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3">
          <option value="">선택해 주세요</option>{accounts.accounts.map((account) => <option key={account.accountUid} value={account.accountUid}>{account.name}</option>)}
        </select>
      </label>
      {sameAccount && <p role="alert" className="text-sm text-red-600">출금 계좌와 입금 계좌는 다르게 선택해 주세요.</p>}
      <label className="block text-sm font-medium">금액
        <input required type="text" inputMode="decimal" value={values.amount} onChange={(event) => setValues((current) => ({ ...current, amount: event.target.value }))}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      <label className="block text-sm font-medium">이체 날짜
        <input required type="date" value={values.transferDate} onChange={(event) => setValues((current) => ({ ...current, transferDate: event.target.value }))}
          className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      </label>
      <label className="block text-sm font-medium">메모 (선택)
        <textarea maxLength={500} rows={3} value={values.memo} onChange={(event) => setValues((current) => ({ ...current, memo: event.target.value }))}
          className="mt-1 w-full rounded-lg border border-zinc-300 px-3 py-2" />
      </label>
      {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
      <div className="flex gap-2">
        <button type="button" onClick={onClose} className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-4">취소</button>
        <button type="submit" disabled={isSaving || accounts.isLoading || accounts.isError || accounts.accounts.length < 2 || sameAccount}
          className="min-h-11 flex-1 rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-50">{isSaving ? "저장 중..." : "저장"}</button>
      </div>
    </form>
  </DialogShell>;
}
