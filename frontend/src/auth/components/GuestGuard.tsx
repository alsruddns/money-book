"use client";

import type { ReactNode } from "react";
import { useRedirectIfAuthenticated } from "../hooks/useRedirectIfAuthenticated";

export default function GuestGuard({ children }: { children: ReactNode }) {
  const { isLoading, isAuthenticated } = useRedirectIfAuthenticated();

  if (isLoading) {
    return <main className="flex flex-1 items-center justify-center">인증 확인 중...</main>;
  }
  if (isAuthenticated) return null;
  return children;
}
