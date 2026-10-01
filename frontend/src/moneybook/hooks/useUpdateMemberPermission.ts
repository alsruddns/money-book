"use client";

import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useUpdateMoneyBookMemberPermissionMutation } from "../controller/moneyBookApi";
import type { UpdateMoneyBookMemberPermissionRequest } from "../dto/req/UpdateMoneyBookMemberPermissionRequest";
import { normalizePermissions } from "../permissions";

export function useUpdateMemberPermission(moneyBookUid: number, moneyBookUserUid: number) {
  const [updateMutation, { isLoading }] = useUpdateMoneyBookMemberPermissionMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function updateMemberPermission(request: UpdateMoneyBookMemberPermissionRequest): Promise<boolean> {
    setErrorMessage(null);
    try {
      await updateMutation({
        moneyBookUid, moneyBookUserUid, request: normalizePermissions(request),
      }).unwrap();
      return true;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "멤버 권한을 변경하지 못했습니다."));
      return false;
    }
  }

  return { updateMemberPermission, isLoading, errorMessage };
}
