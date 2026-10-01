"use client";

import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetPendingInvitationsQuery } from "../controller/moneyBookApi";

export function usePendingInvitations() {
  const { data, isLoading, isError, error } = useGetPendingInvitationsQuery();
  return {
    invitations: (data ?? []).filter((invitation) => invitation.invitationStatus === "PENDING"),
    isLoading,
    isError,
    errorMessage: isError ? getApiErrorMessage(error, "받은 초대를 불러오지 못했습니다.") : null,
  };
}
