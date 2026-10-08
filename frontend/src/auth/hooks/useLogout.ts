"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";


import { useDispatch } from "react-redux";
import { baseApi } from "@/common/api/baseApi";
import { clearLocalSession } from "@/auth/session/clearLocalSession";
import type { AppDispatch } from "@/store/store";
import { useLogoutMutation } from "../controller/authApi";

export function useLogout() {
  const router = useMoneyRouter();
  const dispatch = useDispatch<AppDispatch>();
  const [logoutMutation, { isLoading }] = useLogoutMutation();

  async function logout(): Promise<void> {
    if (isLoading || !window.confirm("이 기기에서 로그아웃하시겠습니까?")) return;
    let serverRevoked = true;
    try {
      await logoutMutation().unwrap();
    } catch {
      serverRevoked = false;
    } finally {
      clearLocalSession(dispatch, () => dispatch(baseApi.util.resetApiState()));
      router.replace(serverRevoked ? "/login?reason=logged-out" : "/login?reason=logout-incomplete");
    }
  }

  return { logout, isLoading };
}
