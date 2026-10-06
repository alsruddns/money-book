"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMoneyBookMembersQuery } from "../controller/moneyBookApi";

export function useMoneyBookMembers(moneyBookUid: number, enabled = true) {
  const { data, isLoading, isError, error } = useGetMoneyBookMembersQuery(moneyBookUid, {
    skip: !enabled,
  });
  return {
    members: data ?? [],
    isLoading,
    isError,
    errorMessage: isError ? getApiErrorMessage(error, "멤버 목록을 불러오지 못했습니다.") : null,
  };
}
