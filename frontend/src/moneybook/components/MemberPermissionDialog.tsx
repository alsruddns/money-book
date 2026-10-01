"use client";

import { useState, type FormEvent } from "react";
import type { MoneyBookMemberResponse } from "../dto/res/MoneyBookMemberResponse";
import type { UpdateMoneyBookMemberPermissionRequest } from "../dto/req/UpdateMoneyBookMemberPermissionRequest";
import { useUpdateMemberPermission } from "../hooks/useUpdateMemberPermission";
import PermissionFields from "./PermissionFields";
import DialogShell from "./DialogShell";

export default function MemberPermissionDialog({ moneyBookUid, member, onClose }: {
  moneyBookUid: number; member: MoneyBookMemberResponse; onClose: () => void;
}) {
  const [permissions, setPermissions] = useState<UpdateMoneyBookMemberPermissionRequest>({
    isAdmin: member.isAdmin,
    canCreate: member.canCreate,
    canRead: member.canRead,
    canUpdate: member.canUpdate,
    canDelete: member.canDelete,
  });
  const { updateMemberPermission, isLoading, errorMessage } = useUpdateMemberPermission(
    moneyBookUid, member.moneyBookUserUid,
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading) return;
    if (await updateMemberPermission(permissions)) onClose();
  }

  return (
    <DialogShell title={`${member.nickname}님 권한 변경`} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-5">
        <PermissionFields idPrefix={`member-${member.moneyBookUserUid}`} value={permissions} onChange={setPermissions} />
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading}
          className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? "저장 중..." : "권한 저장"}
        </button>
      </form>
    </DialogShell>
  );
}
