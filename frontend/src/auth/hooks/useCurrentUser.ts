"use client";

import { useSelector } from "react-redux";
import type { RootState } from "@/store/store";
import { useGetCurrentUserQuery } from "../controller/authApi";

export function useCurrentUser() {
  const { accessToken, refreshToken, isInitialized } = useSelector(
    (state: RootState) => state.auth,
  );
  const hasTokens = Boolean(accessToken || refreshToken);
  const query = useGetCurrentUserQuery(undefined, {
    skip: !isInitialized || !hasTokens,
    refetchOnMountOrArgChange: true,
  });

  return {
    currentUser: query.data ?? null,
    isLoading: !isInitialized || (hasTokens && (query.isUninitialized || query.isFetching)),
    isAuthenticated: hasTokens && query.isSuccess && Boolean(query.data),
  };
}
