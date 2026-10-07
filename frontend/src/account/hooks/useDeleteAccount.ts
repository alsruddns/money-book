"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteAccountMutation } from "../controller/accountApi";

export function useDeleteAccount(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteAccountMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function deleteAccount(accountUid: number, name: string): Promise<boolean> {
    if (!window.confirm(`'${name}' 계좌를 삭제하시겠습니까?`)) return false;
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, accountUid }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "계좌를 삭제하지 못했습니다.")); return false; }
  }
  return { deleteAccount, isLoading, errorMessage };
}
