"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useCreateTransactionMutation } from "../controller/transactionApi";
import { parseTransactionRequest, type TransactionFormValues } from "../transactionForm";

export function useCreateTransaction(moneyBookUid: number) {
  const [trigger, { isLoading }] = useCreateTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function createTransaction(values: TransactionFormValues): Promise<boolean> {
    const parsed = parseTransactionRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, request: parsed.request }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "거래를 등록하지 못했습니다.")); return false; }
  }
  return { createTransaction, isLoading, errorMessage };
}
