"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { validateNamedItem } from "@/common/validation/validateNamedItem";
import { useCreateAccountMutation } from "../controller/accountApi";
import type { CreateAccountRequest } from "../dto/req/CreateAccountRequest";

export function useCreateAccount(moneyBookUid: number) {
  const [trigger, { isLoading }] = useCreateAccountMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function createAccount(request: CreateAccountRequest): Promise<boolean> {
    const validationError = validateNamedItem(request.name, request.sortOrder);
    if (validationError) { setErrorMessage(validationError); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, request: { ...request, name: request.name.trim() } }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "계좌를 등록하지 못했습니다.")); return false; }
  }
  return { createAccount, isLoading, errorMessage };
}
