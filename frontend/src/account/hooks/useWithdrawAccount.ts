"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useDispatch } from "react-redux";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { baseApi } from "@/common/api/baseApi";
import { tokenStorage } from "@/auth/storage/tokenStorage";
import { clearAuth } from "@/auth/store/authSlice";
import type { AppDispatch } from "@/store/store";
import { useWithdrawAccountMutation } from "../controller/accountApi";

function withdrawalError(error: unknown): string {
  if (typeof error === "object" && error !== null && "data" in error) {
    const data = error.data;
    if (typeof data === "object" && data !== null && "code" in data) {
      if (data.code === "OWNED_MONEY_BOOK_EXISTS") return "소유 중인 가계부가 있어 탈퇴할 수 없습니다. 먼저 해당 가계부의 소유권을 다른 멤버에게 이전해주세요.";
      if (data.code === "INVALID_CURRENT_PASSWORD") return "현재 비밀번호가 일치하지 않습니다.";
      if (data.code === "SUPER_ADMIN_WITHDRAWAL_FORBIDDEN") return "최고 관리자는 이 화면에서 탈퇴할 수 없습니다.";
      if (data.code === "LOCAL_AUTH_NOT_FOUND") return "현재 소셜 로그인 계정은 이 화면에서 탈퇴할 수 없습니다.";
    }
  }
  return getApiErrorMessage(error, "회원 탈퇴를 완료하지 못했습니다.");
}

export function useWithdrawAccount() {
  const router = useRouter();
  const dispatch = useDispatch<AppDispatch>();
  const [withdrawMutation, { isLoading }] = useWithdrawAccountMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function withdraw(currentPassword: string): Promise<boolean> {
    if (isLoading || !currentPassword.trim() || new TextEncoder().encode(currentPassword).length > 72) return false;
    setErrorMessage(null);
    try {
      await withdrawMutation({ currentPassword }).unwrap();
      tokenStorage.clearTokens();
      dispatch(clearAuth());
      dispatch(baseApi.util.resetApiState());
      router.replace("/login");
      return true;
    } catch (error) {
      setErrorMessage(withdrawalError(error));
      return false;
    }
  }

  return { withdraw, isLoading, errorMessage };
}
