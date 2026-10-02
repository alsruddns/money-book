"use client";

import type { ReactNode } from "react";
import { useAuthGuard } from "../hooks/useAuthGuard";

export default function AuthGuard({ children }: { children: ReactNode }) {
  const { isLoading, isAuthenticated } = useAuthGuard();

  if (isLoading) {
    return <main className="mx-auto min-h-[40vh] w-full max-w-screen-2xl flex-1 px-4 py-8 sm:px-6"><div role="status" className="min-h-40 animate-pulse rounded-xl border border-zinc-200 bg-white p-6">인증 상태를 확인하고 있습니다...</div></main>;
  }
  if (!isAuthenticated) return <main className="mx-auto min-h-[40vh] w-full max-w-screen-2xl flex-1 px-4 py-8 sm:px-6"><p role="status" className="rounded-xl border border-zinc-200 bg-white p-6">로그인 화면으로 이동하고 있습니다...</p></main>;
  return children;
}
