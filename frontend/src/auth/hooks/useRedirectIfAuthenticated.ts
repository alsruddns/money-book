"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCurrentUser } from "./useCurrentUser";

export function useRedirectIfAuthenticated() {
  const router = useRouter();
  const { isLoading, isAuthenticated } = useCurrentUser();

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace("/books");
  }, [isLoading, isAuthenticated, router]);

  return { isLoading, isAuthenticated };
}
