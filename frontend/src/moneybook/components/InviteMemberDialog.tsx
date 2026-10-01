"use client";

import { useState, type FormEvent } from "react";
import type { CreateInvitationRequest } from "../dto/req/CreateInvitationRequest";
import { useInviteMoneyBookUser } from "../hooks/useInviteMoneyBookUser";
import PermissionFields from "./PermissionFields";
import DialogShell from "@/common/components/DialogShell";

export default function InviteMemberDialog({ moneyBookUid, onClose }: { moneyBookUid: number; onClose: () => void }) {
  const [request, setRequest] = useState<CreateInvitationRequest>({
    loginId: "", isAdmin: false, canCreate: false, canRead: true, canUpdate: false, canDelete: false,
  });
  const { inviteMoneyBookUser, isLoading, errorMessage } = useInviteMoneyBookUser(moneyBookUid);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading) return;
    if (await inviteMoneyBookUser(request)) onClose();
  }

  return (
    <DialogShell title="사용자 초대" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="invite-login-id" className="mb-1 block text-sm font-medium">로그인 ID</label>
          <input id="invite-login-id" required maxLength={100} value={request.loginId} autoFocus
            onChange={(event) => setRequest((current) => ({ ...current, loginId: event.target.value }))}
            className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 outline-none focus:border-blue-600" />
        </div>
        <PermissionFields idPrefix="invite" value={request}
          onChange={(permissions) => setRequest((current) => ({ ...current, ...permissions }))} />
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading}
          className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? "초대 중..." : "초대 보내기"}
        </button>
      </form>
    </DialogShell>
  );
}
