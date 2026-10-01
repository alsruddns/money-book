"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useSignupMutation } from "../controller/authApi";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";
import { getAuthErrorMessage } from "./authError";

export function useSignup() {
  const router = useRouter();
  const [signupMutation, { isLoading }] = useSignupMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function signup(request: SignUpReqDto) {
    if (isLoading) return;
    if (Object.values(request).some((value) => !value.trim())) {
      setErrorMessage("모든 항목을 입력해 주세요.");
      return;
    }
    if (request.password !== request.passwordConfirm) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
      return;
    }
    setErrorMessage(null);
    try {
      await signupMutation(request).unwrap();
      router.push("/login");
    } catch (error) {
      setErrorMessage(getAuthErrorMessage(error, "회원가입에 실패했습니다."));
    }
  }

  return { signup, isLoading, errorMessage };
}
