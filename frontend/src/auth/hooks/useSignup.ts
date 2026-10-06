"use client";

import { useState } from "react";
import { useSignupMutation } from "../controller/authApi";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";
import { getApiErrorCode, getApiErrorMessage } from "@/common/api/getApiErrorMessage";

export function useSignup() {
  const [signupMutation, { isLoading, reset }] = useSignupMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function signup(request: SignUpReqDto, onDuplicateEmail?: () => void) {
    if (isLoading) return;
    if ([request.loginId, request.password, request.passwordConfirm, request.nickname,
      request.securityQuestionCode, request.securityAnswer].some((value) => !value?.trim())) {
      setErrorMessage("필수 항목을 모두 입력해 주세요.");
      return;
    }
    if (request.password !== request.passwordConfirm) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
      return;
    }
    setErrorMessage(null);
    try {
      const result = await signupMutation(request).unwrap();
      reset();
      return result;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "회원가입에 실패했습니다."));
      if (getApiErrorCode(error) === "EMAIL_ALREADY_IN_USE") onDuplicateEmail?.();
      reset();
      return null;
    }
  }

  return { signup, isLoading, errorMessage, reset };
}
