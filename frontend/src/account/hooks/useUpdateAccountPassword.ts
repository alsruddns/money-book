"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { useState } from "react";

import { useDispatch } from "react-redux";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { baseApi } from "@/common/api/baseApi";
import { clearLocalSession } from "@/auth/session/clearLocalSession";
import type { AppDispatch } from "@/store/store";
import type { AccountPasswordUpdateReqDto } from "../dto/req/AccountPasswordUpdateReqDto";
import { validatePasswordUpdate } from "../accountValidation";
import { useUpdateAccountPasswordMutation } from "../controller/accountApi";

export function useUpdateAccountPassword() {
  const router = useMoneyRouter();
  const dispatch = useDispatch<AppDispatch>();
  const [trigger, { isLoading, reset }] = useUpdateAccountPasswordMutation();
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
      reset();
      clearLocalSession(dispatch, () => dispatch(baseApi.util.resetApiState()));
      router.replace("/login?reason=password-changed");
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "비밀번호를 변경하지 못했습니다."));
      reset();
      return false;
    }
  }

  return { updatePassword, isLoading, errorMessage };
}
