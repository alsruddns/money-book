"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import type { AccountPasswordUpdateReqDto } from "../dto/req/AccountPasswordUpdateReqDto";
import { validatePasswordUpdate } from "../accountValidation";
import { useUpdateAccountPasswordMutation } from "../controller/accountApi";

export function useUpdateAccountPassword() {
  const [trigger, { isLoading }] = useUpdateAccountPasswordMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function updatePassword(request: AccountPasswordUpdateReqDto): Promise<boolean> {
    const validationError = validatePasswordUpdate(request);
    if (validationError) {
      setErrorMessage(validationError);
      return false;
    }
    setErrorMessage(null);
    try {
      await trigger(request).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "비밀번호를 변경하지 못했습니다."));
      return false;
    }
  }

  return { updatePassword, isLoading, errorMessage };
}
