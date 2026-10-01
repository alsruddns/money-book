"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useRemoveMoneyBookMemberMutation } from "../controller/moneyBookApi";

export function useRemoveMoneyBookMember(moneyBookUid: number) {
  const [removeMutation, { isLoading }] = useRemoveMoneyBookMemberMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function removeMoneyBookMember(moneyBookUserUid: number, nickname: string) {
    if (!window.confirm(`${nickname}님을 이 가계부에서 제거하시겠습니까?`)) return;
    setErrorMessage(null);
    try {
      await removeMutation({ moneyBookUid, moneyBookUserUid }).unwrap();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "멤버를 제거하지 못했습니다."));
    }
  }

  return { removeMoneyBookMember, isLoading, errorMessage };
}
