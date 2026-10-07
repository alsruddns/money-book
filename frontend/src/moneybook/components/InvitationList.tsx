"use client";

import Link from "next/link";
import { usePendingInvitations } from "../hooks/usePendingInvitations";
import InvitationCard from "./InvitationCard";

export default function InvitationList() {
  const { invitations, isLoading, isError, errorMessage } = usePendingInvitations();

  return (
    <div className="space-y-6">
      <div>
        <Link href="/books" className="text-sm font-medium text-blue-700 hover:underline">← 가계부 목록</Link>
        <h1 className="mt-3 text-2xl font-semibold">받은 초대</h1>
      </div>
      {isLoading ? <p role="status">초대를 불러오는 중...</p> :
        isError ? <p role="alert" className="text-red-600">{errorMessage}</p> :
        invitations.length === 0 ? (
          <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-8 text-center">
            대기 중인 초대가 없습니다.
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {invitations.map((invitation) => (
              <InvitationCard key={invitation.moneyBookUserUid} invitation={invitation} />
            ))}
          </div>
        )}
    </div>
  );
}
