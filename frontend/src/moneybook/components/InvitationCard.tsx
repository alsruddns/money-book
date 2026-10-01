"use client";

import type { PendingInvitationResponse } from "../dto/res/PendingInvitationResponse";
import { useAcceptInvitation } from "../hooks/useAcceptInvitation";
import { useRejectInvitation } from "../hooks/useRejectInvitation";
import PermissionBadges from "./PermissionBadges";

export default function InvitationCard({ invitation }: { invitation: PendingInvitationResponse }) {
  const { acceptInvitation, isLoading: isAccepting, errorMessage: acceptError } = useAcceptInvitation();
  const { rejectInvitation, isLoading: isRejecting, errorMessage: rejectError } = useRejectInvitation();
  const isLoading = isAccepting || isRejecting;

  return (
    <article className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-semibold">{invitation.moneyBookName}</h2>
      <p className="mt-1 text-sm text-zinc-600">소유자 UID: {invitation.ownerUserUid}</p>
      <div className="mt-3"><PermissionBadges permissions={invitation} /></div>
      {(acceptError || rejectError) && <p role="alert" className="mt-3 text-sm text-red-600">{acceptError || rejectError}</p>}
      <div className="mt-5 flex gap-2">
        <button type="button" disabled={isLoading}
          onClick={() => void acceptInvitation(invitation.moneyBookUid, invitation.moneyBookUserUid)}
          className="min-h-11 flex-1 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:opacity-60">
          {isAccepting ? "수락 중..." : "수락"}
        </button>
        <button type="button" disabled={isLoading}
          onClick={() => void rejectInvitation(invitation.moneyBookUid, invitation.moneyBookUserUid)}
          className="min-h-11 flex-1 rounded-lg border border-zinc-300 px-4 text-sm font-medium disabled:opacity-60">
          {isRejecting ? "거절 중..." : "거절"}
        </button>
      </div>
    </article>
  );
}
