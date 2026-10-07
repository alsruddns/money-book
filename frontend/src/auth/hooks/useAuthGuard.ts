"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { useEffect } from "react";

import { useCurrentUser } from "./useCurrentUser";
import { usePathname } from "next/navigation";

export function useAuthGuard() {
  const router = useMoneyRouter();
  const pathname = usePathname();
  const { isLoading, isAuthenticated, currentUser } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace("/login");
    else if (!isLoading && isAuthenticated && currentUser?.passwordChangeRequired && pathname !== "/change-required-password") router.replace("/change-required-password");
  }, [isLoading, isAuthenticated, currentUser?.passwordChangeRequired, pathname, router]);

  return { isLoading, isAuthenticated, passwordChangeRequired: currentUser?.passwordChangeRequired === true, pathname };
}
