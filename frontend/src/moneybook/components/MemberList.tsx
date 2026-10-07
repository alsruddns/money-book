"use client";

import { useState } from "react";
import Link from "next/link";
import { useMoneyBookDetail } from "../hooks/useMoneyBookDetail";
import { useMoneyBookMembers } from "../hooks/useMoneyBookMembers";
import InviteMemberDialog from "./InviteMemberDialog";
import MemberRow from "./MemberRow";
import { useTransferMoneyBookOwner } from "../hooks/useTransferMoneyBookOwner";

export default function MemberList({ moneyBookUid }: { moneyBookUid: number }) {
  const [isInviteOpen, setInviteOpen] = useState(false);
  const [targetUserUid, setTargetUserUid] = useState("");
  const [transferSuccess, setTransferSuccess] = useState(false);
  const { moneyBook, isLoading: isBookLoading, isError: isBookError, errorMessage: bookError } = useMoneyBookDetail(moneyBookUid);
  const { members, isLoading, isError, errorMessage } = useMoneyBookMembers(moneyBookUid, Boolean(moneyBook));
  const ownerTransfer = useTransferMoneyBookOwner(moneyBookUid);

  if (isBookLoading) return <p role="status">가계부를 불러오는 중...</p>;
  if (isBookError) return <p role="alert" className="text-red-600">{bookError}</p>;
  if (!moneyBook) return <p role="alert">접근 가능한 가계부를 찾을 수 없습니다.</p>;

  const canManage = moneyBook.isOwner || moneyBook.isAdmin;
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <Link href={`/books/${moneyBookUid}`} className="text-sm font-medium text-blue-700 hover:underline">← {moneyBook.name}</Link>
          <h1 className="mt-3 text-2xl font-semibold">멤버 목록</h1>
          <p className="mt-1 text-sm text-zinc-600">가계부 멤버와 권한을 확인합니다.</p>
        </div>
        {canManage && <button type="button" onClick={() => setInviteOpen(true)}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700">
          사용자 초대
        </button>}
      </div>
      {isLoading ? <p role="status">멤버를 불러오는 중...</p> :
        isError ? <p role="alert" className="text-red-600">{errorMessage}</p> :
        members.length === 0 ? <p>가입한 멤버가 없습니다.</p> : (
          <div className="space-y-3">
            {members.map((member) => (
              <MemberRow key={member.moneyBookUserUid} member={member}
                moneyBookUid={moneyBookUid} canManage={canManage} />
            ))}
          </div>
        )}
      {moneyBook.isOwner && !isLoading && !isError && <section aria-labelledby="owner-transfer-title" className="rounded-xl border border-amber-300 bg-amber-50 p-5">
        <h2 id="owner-transfer-title" className="text-lg font-semibold">소유권 이전</h2>
        <p className="mt-2 text-sm text-zinc-700">이전 대상은 이 가계부에 가입한 멤버 중에서 선택할 수 있습니다. 활성 계정 여부는 서버에서 확인합니다.</p>
        {members.filter((member) => !member.isOwner).length === 0 ? <p className="mt-3 text-sm text-zinc-700">이전할 수 있는 멤버가 없습니다.</p> : <div className="mt-4 flex flex-col gap-3 sm:flex-row">
          <label className="sr-only" htmlFor="owner-transfer-target">새 소유자</label>
          <select id="owner-transfer-target" value={targetUserUid} onChange={(event) => { setTargetUserUid(event.target.value); setTransferSuccess(false); }} className="min-h-11 min-w-0 flex-1 rounded-lg border border-zinc-300 bg-white px-3">
            <option value="">멤버 선택</option>
            {members.filter((member) => !member.isOwner).map((member) => <option key={member.userUid} value={member.userUid}>{member.nickname}</option>)}
          </select>
          <button type="button" disabled={!targetUserUid || ownerTransfer.isLoading} onClick={async () => {
            const target = members.find((member) => member.userUid === Number(targetUserUid));
            if (target && await ownerTransfer.transferOwner(target.userUid, target.nickname)) setTransferSuccess(true);
          }} className="min-h-11 rounded-lg border border-amber-700 px-4 text-sm font-medium text-amber-950 disabled:opacity-50">
            {ownerTransfer.isLoading ? "이전 중..." : "소유권 이전"}
          </button>
        </div>}
        {ownerTransfer.errorMessage && <p role="alert" className="mt-3 text-sm text-red-700">{ownerTransfer.errorMessage}</p>}
        {transferSuccess && <p role="status" className="mt-3 text-sm text-green-800">소유권을 이전했습니다.</p>}
      </section>}
      {isInviteOpen && canManage && (
        <InviteMemberDialog moneyBookUid={moneyBookUid} onClose={() => setInviteOpen(false)} />
      )}
    </div>
  );
}
