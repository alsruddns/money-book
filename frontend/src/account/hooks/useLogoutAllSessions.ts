"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useDispatch } from "react-redux";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { baseApi } from "@/common/api/baseApi";
import { clearLocalSession } from "@/auth/session/clearLocalSession";
import type { AppDispatch } from "@/store/store";
import { useLogoutAllAccountSessionsMutation } from "../controller/accountApi";

export function useLogoutAllSessions() {
  const router = useRouter();
  const dispatch = useDispatch<AppDispatch>();
  const [logoutAllMutation, { isLoading }] = useLogoutAllAccountSessionsMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function logoutAll(): Promise<boolean> {
    if (isLoading || !window.confirm("현재 기기를 포함한 모든 로그인 세션이 종료됩니다. 계속하시겠습니까?")) return false;
    setErrorMessage(null);
    try {
      await logoutAllMutation().unwrap();
      clearLocalSession(dispatch, () => dispatch(baseApi.util.resetApiState()));
      router.replace("/login?reason=sessions-ended");
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "모든 세션을 종료하지 못했습니다."));
      return false;
    }
  }

  return { logoutAll, isLoading, errorMessage };
}
