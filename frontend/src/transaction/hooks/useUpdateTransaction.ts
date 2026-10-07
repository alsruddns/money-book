"use client";

import { useState } from "react";
import { getTransactionErrorMessage } from "../transactionError";
import { useUpdateTransactionMutation } from "../controller/transactionApi";
import { parseTransactionRequest, type TransactionFormValues } from "../transactionForm";

export function useUpdateTransaction(moneyBookUid: number, transactionUid: number | null) {
  const [trigger, { isLoading }] = useUpdateTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function updateTransaction(values: TransactionFormValues): Promise<boolean> {
    if (transactionUid === null) return false;
    const parsed = parseTransactionRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, transactionUid, request: parsed.request }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getTransactionErrorMessage(error, "거래를 수정하지 못했습니다.")); return false; }
  }
  return { updateTransaction, isLoading, errorMessage };
}
