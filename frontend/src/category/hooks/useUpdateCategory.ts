"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { validateNamedItem } from "@/common/validation/validateNamedItem";
import { useUpdateCategoryMutation } from "../controller/categoryApi";
import type { UpdateCategoryRequest } from "../dto/req/UpdateCategoryRequest";

export function useUpdateCategory(moneyBookUid: number, categoryUid: number) {
  const [trigger, { isLoading }] = useUpdateCategoryMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function updateCategory(request: UpdateCategoryRequest): Promise<boolean> {
    const validationError = validateNamedItem(request.name, request.sortOrder);
    if (validationError) { setErrorMessage(validationError); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, categoryUid, request: { ...request, name: request.name.trim() } }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "카테고리를 수정하지 못했습니다.")); return false; }
  }
  return { updateCategory, isLoading, errorMessage };
}
