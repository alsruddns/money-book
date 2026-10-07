"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { validateNickname } from "../accountValidation";
import { useUpdateAccountProfileMutation } from "../controller/accountApi";

export function useUpdateAccountProfile() {
  const [trigger, { isLoading }] = useUpdateAccountProfileMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function updateNickname(value: string): Promise<boolean> {
    const nickname = value.trim();
    const validationError = validateNickname(nickname);
    if (validationError) {
      setErrorMessage(validationError);
      return false;
    }
    setErrorMessage(null);
    try {
      await trigger({ nickname }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "닉네임을 변경하지 못했습니다."));
      return false;
    }
  }

  return { updateNickname, isLoading, errorMessage };
}
