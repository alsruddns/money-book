"use client";

import { useState } from "react";
import type { MoneyBookMemberResponse } from "../dto/res/MoneyBookMemberResponse";
import { useRemoveMoneyBookMember } from "../hooks/useRemoveMoneyBookMember";
import PermissionBadges from "./PermissionBadges";
import MemberPermissionDialog from "./MemberPermissionDialog";

export default function MemberRow({ member, moneyBookUid, canManage }: {
  member: MoneyBookMemberResponse; moneyBookUid: number; canManage: boolean;
}) {
  const [isPermissionOpen, setPermissionOpen] = useState(false);
  const { removeMoneyBookMember, isLoading, errorMessage } = useRemoveMoneyBookMember(moneyBookUid);
  const canEdit = canManage && !member.isOwner;

  return (
    <article className="rounded-xl border border-zinc-200 bg-white p-4 sm:p-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-semibold break-words">{member.nickname}</h3>
            {member.isOwner && <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-900">소유자</span>}
          </div>
          <div className="mt-3"><PermissionBadges permissions={member} /></div>
        </div>
        {canEdit && (
          <div className="flex shrink-0 gap-2">
            <button type="button" onClick={() => setPermissionOpen(true)}
              className="min-h-11 rounded-lg border border-zinc-300 px-3 text-sm font-medium hover:bg-zinc-50">
              권한 변경
            </button>
            <button type="button" disabled={isLoading}
              onClick={() => void removeMoneyBookMember(member.moneyBookUserUid, member.nickname)}
              className="min-h-11 rounded-lg border border-red-200 px-3 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-60">
              {isLoading ? "제거 중..." : "제거"}
            </button>
          </div>
        )}
      </div>
      {errorMessage && <p role="alert" className="mt-3 text-sm text-red-600">{errorMessage}</p>}
      {isPermissionOpen && canEdit && (
        <MemberPermissionDialog moneyBookUid={moneyBookUid} member={member}
          onClose={() => setPermissionOpen(false)} />
      )}
    </article>
  );
}
