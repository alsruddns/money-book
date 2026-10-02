"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useCreateRecurringTransactionMutation } from "../controller/recurringTransactionApi";
import { parseRecurringRequest, type RecurringFormValues } from "../recurringForm";

export function useCreateRecurringTransaction(moneyBookUid: number) {
  const [trigger, { isLoading }] = useCreateRecurringTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function create(values: RecurringFormValues): Promise<boolean> {
    if (isLoading) return false;
    const parsed = parseRecurringRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, request: parsed.request }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "정기 규칙을 등록하지 못했습니다.")); return false; }
  }
  return { create, isLoading, errorMessage };
}
