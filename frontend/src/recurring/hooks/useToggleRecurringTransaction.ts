"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useChangeRecurringTransactionActiveMutation } from "../controller/recurringTransactionApi";

export function useToggleRecurringTransaction(moneyBookUid: number) {
  const [trigger, { isLoading }] = useChangeRecurringTransactionActiveMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function toggle(recurringTransactionUid: number, active: boolean): Promise<boolean> {
    if (isLoading) return false;
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, recurringTransactionUid, request: { active } }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "활성 상태를 변경하지 못했습니다.")); return false; }
  }
  return { toggle, isLoading, errorMessage };
}
