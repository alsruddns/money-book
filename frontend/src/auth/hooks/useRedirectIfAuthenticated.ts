"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCurrentUser } from "./useCurrentUser";

export function useRedirectIfAuthenticated() {
  const router = useRouter();
  const { isLoading, isAuthenticated, currentUser } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace(currentUser?.passwordChangeRequired ? "/change-required-password" : "/books");
  }, [isLoading, isAuthenticated, currentUser?.passwordChangeRequired, router]);

  return { isLoading, isAuthenticated };
}
