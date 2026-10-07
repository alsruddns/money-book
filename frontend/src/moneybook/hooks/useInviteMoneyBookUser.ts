"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useInviteMoneyBookUserMutation } from "../controller/moneyBookApi";
import type { CreateInvitationRequest } from "../dto/req/CreateInvitationRequest";
import { normalizePermissions } from "../permissions";

export function useInviteMoneyBookUser(moneyBookUid: number) {
  const [inviteMutation, { isLoading }] = useInviteMoneyBookUserMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function inviteMoneyBookUser(request: CreateInvitationRequest): Promise<boolean> {
    const loginId = request.loginId.trim();
    if (!loginId) {
      setErrorMessage("초대할 사용자의 로그인 ID를 입력해 주세요.");
      return false;
    }
    setErrorMessage(null);
    try {
      await inviteMutation({
        moneyBookUid,
        request: { ...normalizePermissions(request), loginId },
      }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "사용자를 초대하지 못했습니다."));
      return false;
    }
  }

  return { inviteMoneyBookUser, isLoading, errorMessage };
}
