"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteRecurringTransactionMutation } from "../controller/recurringTransactionApi";

export function useDeleteRecurringTransaction(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteRecurringTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function remove(recurringTransactionUid: number): Promise<boolean> {
    if (isLoading || !window.confirm("정기 규칙을 삭제하시겠습니까? 이미 생성된 거래는 삭제되지 않습니다.")) return false;
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, recurringTransactionUid }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "정기 규칙을 삭제하지 못했습니다.")); return false; }
  }
  return { remove, isLoading, errorMessage };
}
