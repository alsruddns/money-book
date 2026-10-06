"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useTransferMoneyBookOwnerMutation } from "../controller/moneyBookApi";

export function useTransferMoneyBookOwner(moneyBookUid: number) {
  const [transferMutation, { isLoading }] = useTransferMoneyBookOwnerMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function transferOwner(targetUserUid: number, nickname: string): Promise<boolean> {
    if (!Number.isSafeInteger(targetUserUid) || targetUserUid <= 0) return false;
    if (!window.confirm(`가계부 소유권을 ${nickname}님에게 이전하시겠습니까?\n이전 후에도 현재 계정은 멤버로 남습니다.`)) return false;
    setErrorMessage(null);
    try {
      await transferMutation({ moneyBookUid, request: { targetUserUid } }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "가계부 소유권을 이전하지 못했습니다."));
      return false;
    }
  }

  return { transferOwner, isLoading, errorMessage };
}
