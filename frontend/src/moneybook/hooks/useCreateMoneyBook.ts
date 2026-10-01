"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useCreateMoneyBookMutation } from "../controller/moneyBookApi";

export function useCreateMoneyBook() {
  const [createMutation, { isLoading }] = useCreateMoneyBookMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function createMoneyBook(name: string): Promise<boolean> {
    const trimmedName = name.trim();
    if (!trimmedName) {
      setErrorMessage("가계부 이름을 입력해 주세요.");
      return false;
    }
    setErrorMessage(null);
    try {
      await createMutation({ name: trimmedName }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "가계부를 만들지 못했습니다."));
      return false;
    }
  }

  return { createMoneyBook, isLoading, errorMessage };
}
