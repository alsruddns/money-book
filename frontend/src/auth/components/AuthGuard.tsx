"use client";

import type { ReactNode } from "react";
import { useAuthGuard } from "../hooks/useAuthGuard";

export default function AuthGuard({ children }: { children: ReactNode }) {
  const { isLoading, isAuthenticated } = useAuthGuard();

  if (isLoading) {
    return <main className="flex flex-1 items-center justify-center">인증 확인 중...</main>;
  }
  if (!isAuthenticated) return null;
  return children;
}
