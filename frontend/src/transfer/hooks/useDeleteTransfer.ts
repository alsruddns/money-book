"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteTransferMutation } from "../controller/transferApi";

export function useDeleteTransfer(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteTransferMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function remove(transferUid: number): Promise<boolean> {
    if (isLoading || !window.confirm("이 이체를 삭제하시겠습니까?")) return false;
    setErrorMessage(null);
    try { await trigger({ moneyBookUid, transferUid }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "이체를 삭제하지 못했습니다.")); return false; }
  }
  return { remove, isLoading, errorMessage };
}
