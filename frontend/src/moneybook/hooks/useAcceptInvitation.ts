"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useAcceptInvitationMutation } from "../controller/moneyBookApi";

export function useAcceptInvitation() {
  const [acceptMutation, { isLoading }] = useAcceptInvitationMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function acceptInvitation(moneyBookUid: number, moneyBookUserUid: number) {
    setErrorMessage(null);
    try {
      await acceptMutation({ moneyBookUid, moneyBookUserUid }).unwrap();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "초대를 수락하지 못했습니다."));
    }
  }

  return { acceptInvitation, isLoading, errorMessage };
}
