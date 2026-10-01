"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useRejectInvitationMutation } from "../controller/moneyBookApi";

export function useRejectInvitation() {
  const [rejectMutation, { isLoading }] = useRejectInvitationMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function rejectInvitation(moneyBookUid: number, moneyBookUserUid: number) {
    setErrorMessage(null);
    try {
      await rejectMutation({ moneyBookUid, moneyBookUserUid }).unwrap();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "초대를 거절하지 못했습니다."));
    }
  }

  return { rejectInvitation, isLoading, errorMessage };
}
