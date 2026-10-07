"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { validateNamedItem } from "@/common/validation/validateNamedItem";
import { useUpdateAccountMutation } from "../controller/accountApi";
import type { UpdateAccountRequest } from "../dto/req/UpdateAccountRequest";

export function useUpdateAccount(moneyBookUid: number, accountUid: number) {
  const [trigger, { isLoading }] = useUpdateAccountMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function updateAccount(request: UpdateAccountRequest): Promise<boolean> {
    const validationError = validateNamedItem(request.name, request.sortOrder);
    if (validationError) { setErrorMessage(validationError); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, accountUid, request: { ...request, name: request.name.trim() } }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "계좌를 수정하지 못했습니다.")); return false; }
  }
  return { updateAccount, isLoading, errorMessage };
}
