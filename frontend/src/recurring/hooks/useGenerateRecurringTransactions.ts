"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { formatCount } from "@/common/format/money";
import { isLocalDate } from "@/transaction/transactionForm";
import { useGenerateRecurringTransactionsMutation } from "../controller/recurringTransactionApi";

export function useGenerateRecurringTransactions(moneyBookUid: number) {
  const [trigger, { isLoading }] = useGenerateRecurringTransactionsMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  async function generate(baseDate: string): Promise<boolean> {
    if (isLoading) return false;
    setFeedback(null);
    if (!isLocalDate(baseDate)) { setErrorMessage("올바른 기준일을 입력해 주세요."); return false; }
    setErrorMessage(null);
    try {
      const result = await trigger({ moneyBookUid, request: { baseDate } }).unwrap();
      setFeedback(result.generatedCount === 0 ? "새로 생성할 거래가 없습니다." : `정기 거래 ${formatCount(result.generatedCount)}\uC774 \uBC18\uC601\uB418\uC5C8\uC2B5\uB2C8\uB2E4.`);
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "정기 거래를 반영하지 못했습니다.")); return false; }
  }
  return { generate, isLoading, errorMessage, feedback };
}
