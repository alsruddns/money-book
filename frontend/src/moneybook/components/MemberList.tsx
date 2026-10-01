"use client";

import { useState } from "react";
import Link from "next/link";
import { useMoneyBookDetail } from "../hooks/useMoneyBookDetail";
import { useMoneyBookMembers } from "../hooks/useMoneyBookMembers";
import InviteMemberDialog from "./InviteMemberDialog";
import MemberRow from "./MemberRow";

export default function MemberList({ moneyBookUid }: { moneyBookUid: number }) {
  const [isInviteOpen, setInviteOpen] = useState(false);
  const { moneyBook, isLoading: isBookLoading, isError: isBookError, errorMessage: bookError } = useMoneyBookDetail(moneyBookUid);
  const { members, isLoading, isError, errorMessage } = useMoneyBookMembers(moneyBookUid, Boolean(moneyBook));

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
      {isInviteOpen && canManage && (
        <InviteMemberDialog moneyBookUid={moneyBookUid} onClose={() => setInviteOpen(false)} />
      )}
    </div>
  );
}
