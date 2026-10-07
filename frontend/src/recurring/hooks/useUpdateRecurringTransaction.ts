"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useUpdateRecurringTransactionMutation } from "../controller/recurringTransactionApi";
import { parseRecurringRequest, type RecurringFormValues } from "../recurringForm";

export function useUpdateRecurringTransaction(moneyBookUid: number, recurringTransactionUid: number | null) {
  const [trigger, { isLoading }] = useUpdateRecurringTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function update(values: RecurringFormValues): Promise<boolean> {
    if (isLoading || recurringTransactionUid === null) return false;
    const parsed = parseRecurringRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, recurringTransactionUid, request: parsed.request }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "정기 규칙을 수정하지 못했습니다.")); return false; }
  }
  return { update, isLoading, errorMessage };
}
