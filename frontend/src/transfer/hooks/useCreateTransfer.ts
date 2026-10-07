"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useCreateTransferMutation } from "../controller/transferApi";
import { parseTransferRequest, type TransferFormValues } from "../transferForm";

export function useCreateTransfer(moneyBookUid: number) {
  const [trigger, { isLoading }] = useCreateTransferMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function create(values: TransferFormValues): Promise<boolean> {
    if (isLoading) return false;
    const parsed = parseTransferRequest(values);
    if (!parsed.request) { setErrorMessage(parsed.error); return false; }
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, request: parsed.request }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "이체를 등록하지 못했습니다.")); return false; }
  }
  return { create, isLoading, errorMessage };
}
