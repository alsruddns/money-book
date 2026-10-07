"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useSaveMonthlyBudgetMutation } from "../controller/budgetApi";
import { validateBudgetForm, type BudgetCategoryInput } from "../budgetForm";

export function useSaveBudget(moneyBookUid: number, year: number, month: number) {
  const [saveMutation, { isLoading }] = useSaveMonthlyBudgetMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function save(totalText: string, categories: BudgetCategoryInput[]): Promise<boolean> {
    if (isLoading) return false;
    const validated = validateBudgetForm(totalText, categories);
    if (!validated.request) { setErrorMessage(validated.error ?? "예산 입력을 확인해 주세요."); return false; }
    setErrorMessage(null);
    try {
      await saveMutation({ moneyBookUid, year, month, request: validated.request }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "예산을 저장하지 못했습니다."));
      return false;
    }
  }
  return { save, isLoading, errorMessage };
}
