"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useUpdateTransferMutation } from "../controller/transferApi";
import { parseTransferRequest, type TransferFormValues } from "../transferForm";

export function useUpdateTransfer(moneyBookUid: number, transferUid: number | null) {
  const [trigger, { isLoading }] = useUpdateTransferMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function update(values: TransferFormValues): Promise<boolean> {
    if (isLoading || transferUid === null) return false;
    const parsed = parseTransferRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, transferUid, request: parsed.request }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "이체를 수정하지 못했습니다.")); return false; }
  }
  return { update, isLoading, errorMessage };
}
