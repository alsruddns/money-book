"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useRevokeAccountSessionMutation } from "../controller/accountApi";

export function useRevokeAccountSession() {
  const [revokeMutation, { isLoading }] = useRevokeAccountSessionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function revoke(sessionUid: number): Promise<boolean> {
    if (!Number.isSafeInteger(sessionUid) || sessionUid <= 0) return false;
    if (!window.confirm("이 세션에서 로그아웃하시겠습니까?")) return false;
    setErrorMessage(null);
    try {
      await revokeMutation(sessionUid).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "세션을 종료하지 못했습니다."));
      return false;
    }
  }

  return { revoke, isLoading, errorMessage };
}
