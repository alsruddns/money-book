"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { useEffect } from "react";

import { useCurrentUser } from "./useCurrentUser";

export function useRedirectIfAuthenticated() {
  const router = useMoneyRouter();
  const { isLoading, isAuthenticated, currentUser } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace(currentUser?.passwordChangeRequired ? "/change-required-password" : "/books");
  }, [isLoading, isAuthenticated, currentUser?.passwordChangeRequired, router]);

  return { isLoading, isAuthenticated };
}
