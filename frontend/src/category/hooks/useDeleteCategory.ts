"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useDeleteCategoryMutation } from "../controller/categoryApi";
import { useTranslation } from "@/i18n/useTranslation";

export function useDeleteCategory(moneyBookUid: number) {
  const [trigger, { isLoading }] = useDeleteCategoryMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { t } = useTranslation();
  async function deleteCategory(categoryUid: number, name: string, hasChildren = false): Promise<boolean> {
    const confirmText = t(hasChildren ? "category.deleteTreeConfirm" : "category.deleteConfirm").replaceAll("{name}", name);
    if (!window.confirm(confirmText)) return false;
    setErrorMessage(null);
    try {
      await trigger({ moneyBookUid, categoryUid }).unwrap();
      return true;
    } catch (error) { setErrorMessage(getApiErrorMessage(error, "카테고리를 삭제하지 못했습니다.")); return false; }
  }
  return { deleteCategory, isLoading, errorMessage };
}
