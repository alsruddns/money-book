"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useDispatch } from "react-redux";
import { useLoginMutation } from "../controller/authApi";
import type { LoginRequest } from "../dto/req/LoginRequest";
import { setTokens } from "../store/authSlice";
import { tokenStorage } from "../storage/tokenStorage";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import type { AppDispatch } from "@/store/store";
import { baseApi } from "@/common/api/baseApi";

export function useLogin() {
  const router = useRouter();
  const dispatch = useDispatch<AppDispatch>();
  const [loginMutation, { isLoading }] = useLoginMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function login(request: LoginRequest) {
    if (isLoading) return;
    setErrorMessage(null);
    try {
      const response = await loginMutation(request).unwrap();
      const tokens = {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
      };
      tokenStorage.setTokens(tokens);
      dispatch(setTokens(tokens));
      dispatch(baseApi.util.resetApiState());
      router.push("/books");
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "로그인에 실패했습니다."));
    }
  }

  return { login, isLoading, errorMessage };
}
