"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { validateNamedItem } from "@/common/validation/validateNamedItem";
import { useCreateCategoryMutation } from "../controller/categoryApi";
import type { CreateCategoryRequest } from "../dto/req/CreateCategoryRequest";

export function useCreateCategory(moneyBookUid: number) {
  const [trigger, { isLoading }] = useCreateCategoryMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function createCategory(request: CreateCategoryRequest): Promise<boolean> {
    const validationError = validateNamedItem(request.name, request.sortOrder);
    if (validationError) { setErrorMessage(validationError); return false; }
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, request: { ...request, name: request.name.trim() } }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "카테고리를 등록하지 못했습니다.")); return false; }
  }
  return { createCategory, isLoading, errorMessage };
}
