"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteCategoryMutation } from "../controller/categoryApi";

export function useDeleteCategory(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteCategoryMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function deleteCategory(categoryUid: number, name: string): Promise<boolean> {
    if (!window.confirm(`'${name}' 카테고리를 삭제하시겠습니까?`)) return false;
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, categoryUid }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "카테고리를 삭제하지 못했습니다.")); return false; }
  }
  return { deleteCategory, isLoading, errorMessage };
}
