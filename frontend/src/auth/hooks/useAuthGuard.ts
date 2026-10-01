"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCurrentUser } from "./useCurrentUser";

export function useAuthGuard() {
  const router = useRouter();
  const { isLoading, isAuthenticated } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace("/login");
  }, [isLoading, isAuthenticated, router]);

  return { isLoading, isAuthenticated };
}
