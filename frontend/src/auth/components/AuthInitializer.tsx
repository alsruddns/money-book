"use client";

import type { ReactNode } from "react";
import { useAuthInitialize } from "../hooks/useAuthInitialize";
import IdleSessionManager from "./IdleSessionManager";

export default function AuthInitializer({ children }: { children: ReactNode }) {
  useAuthInitialize();
  return <>{children}<IdleSessionManager /></>;
}
