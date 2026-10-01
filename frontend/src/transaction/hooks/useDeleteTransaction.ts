"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteTransactionMutation } from "../controller/transactionApi";

export function useDeleteTransaction(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteTransactionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function deleteTransaction(transactionUid: number): Promise<boolean> {
    if (!window.confirm("이 거래를 삭제하시겠습니까?")) return false;
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, transactionUid }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "거래를 삭제하지 못했습니다.")); return false; }
  }
  return { deleteTransaction, isLoading, errorMessage };
}
